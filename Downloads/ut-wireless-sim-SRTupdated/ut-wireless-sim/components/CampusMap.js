'use client';
import { useEffect, useRef, useCallback, useState } from 'react';

const UT_CENTER = [30.2849, -97.7341];
const UT_ZOOM = 17;
const BACKEND_URL = process.env.NEXT_PUBLIC_BACKEND_URL || 'http://localhost:8000';

// Matches USC visual style — one colour per station
const STATION_COLORS = [
  { hex: '#F5963C', r: 245, g: 150, b: 60 },
  { hex: '#5BA4F5', r: 91, g: 164, b: 245 },
  { hex: '#4DC87A', r: 77, g: 200, b: 122 },
  { hex: '#C060D8', r: 192, g: 96, b: 216 },
  { hex: '#F55A5A', r: 245, g: 90, b: 90 },
  { hex: '#50D4D4', r: 80, g: 212, b: 212 },
];

export default function CampusMap({ stations, setStations, params,
  onStatsUpdate, onMousePos, onEngineUpdate }) {
  const mapRef = useRef(null);
  const leafletRef = useRef(null);
  const canvasRef = useRef(null);
  const markersRef = useRef([]);
  const initRef = useRef(false);
  const cellsRef = useRef([]);
  const redrawRef = useRef(null);   // always points to latest redraw fn
  const [loading, setLoading] = useState(false);

  // ── Render cells onto canvas ──────────────────────────────────────────────
  // Uses small dots (like USC sim) — denser near station, sparse at edge
  const drawCells = useCallback((cells) => {
    const map = leafletRef.current;
    const canvas = canvasRef.current;
    if (!map || !canvas) return;

    const ctx = canvas.getContext('2d');
    const w = canvas.width, h = canvas.height;
    ctx.clearRect(0, 0, w, h);
    if (!cells?.length) return;

    // RSSI range for normalisation
    const rssiArr = cells.map(c => c.rssi);
    const rssiMin = Math.min(...rssiArr);
    const rssiMax = Math.max(...rssiArr);
    const rssiSpan = Math.max(1, rssiMax - rssiMin);

    // Dot radius: ~3px — same compact feel as USC
    const DOT_R = 3;

    for (const cell of cells) {
      const pt = map.latLngToContainerPoint([cell.lat, cell.lng]);
      if (pt.x < -DOT_R || pt.y < -DOT_R || pt.x > w + DOT_R || pt.y > h + DOT_R) continue;

      const t = (cell.rssi - rssiMin) / rssiSpan;   // 0 = weakest, 1 = strongest
      const alpha = 0.30 + t * 0.65;                     // 0.30–0.95

      const col = STATION_COLORS[cell.station_idx % STATION_COLORS.length];
      ctx.fillStyle = `rgba(${col.r},${col.g},${col.b},${alpha.toFixed(2)})`;
      ctx.beginPath();
      ctx.arc(pt.x, pt.y, DOT_R, 0, Math.PI * 2);
      ctx.fill();
    }
  }, []);

  // ── Fetch coverage from backend ───────────────────────────────────────────
  const fetchCoverage = useCallback(async (sts, prms) => {
    if (!sts.length) {
      cellsRef.current = [];
      drawCells([]);
      onStatsUpdate({ coverage: 0, maxRange: 0 });
      return;
    }
    setLoading(true);
    try {
      const res = await fetch(`${BACKEND_URL}/coverage`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          stations: sts.map(s => ({ id: String(s.id), lat: s.lat, lng: s.lng })),
          tx_power_dbm: prms.txPower,
          frequency_ghz: prms.freq,
          noise_floor_dbm: prms.noiseFloor,
          snr_threshold_db: prms.snrThreshold,
        }),
      });
      if (!res.ok) throw new Error(`HTTP ${res.status}`);
      const data = await res.json();
      cellsRef.current = data.cells;
      drawCells(data.cells);
      onStatsUpdate({ coverage: data.coverage_pct, maxRange: data.max_range_m });
      if (onEngineUpdate) onEngineUpdate(data.engine);
    } catch (err) {
      console.error('Coverage fetch:', err);
    } finally {
      setLoading(false);
    }
  }, [drawCells, onStatsUpdate, onEngineUpdate]);

  const redraw = useCallback(() => drawCells(cellsRef.current), [drawCells]);
  // Keep ref in sync so CanvasLayer listeners always call the latest version
  redrawRef.current = redraw;

  // ── Init Leaflet ──────────────────────────────────────────────────────────
  useEffect(() => {
    if (initRef.current || !mapRef.current) return;
    initRef.current = true;

    import('leaflet').then(mod => {
      const L = mod.default || mod;
      if (leafletRef.current) return;

      delete L.Icon.Default.prototype._getIconUrl;

      const map = L.map(mapRef.current, {
        center: UT_CENTER, zoom: UT_ZOOM,
        zoomControl: false,
        scrollWheelZoom: false, doubleClickZoom: false,
        touchZoom: false, boxZoom: false, keyboard: false,
        dragging: true,
      });
      leafletRef.current = map;

      // Base: white, no labels
      L.tileLayer('https://{s}.basemaps.cartocdn.com/light_nolabels/{z}/{x}/{y}{r}.png', {
        subdomains: 'abcd', maxZoom: 19,
        attribution: '© OpenStreetMap © CARTO',
      }).addTo(map);

      // Canvas heatmap layer — mounted directly on the map container so it
      // is never affected by Leaflet's pane CSS transforms during pan/zoom.
      const CanvasLayer = L.Layer.extend({
        onAdd(m) {
          const c = document.createElement('canvas');
          Object.assign(c.style, {
            position: 'absolute', top: '0', left: '0',
            pointerEvents: 'none',
            zIndex: '400',
          });
          // Attach to the map container itself, not a transformed pane
          m.getContainer().appendChild(c);
          canvasRef.current = c;

          const stableRedraw = () => redrawRef.current && redrawRef.current();

          const resize = () => {
            const s = m.getSize();
            c.width = s.x;
            c.height = s.y;
            stableRedraw();
          };

          m.on('resize', resize);
          m.on('viewreset', stableRedraw);
          m.on('move', stableRedraw);
          resize();
        },
        onRemove() {
          // stableRedraw is closure-local; Leaflet cleans up listeners w/ the layer
          const c = canvasRef.current;
          if (c && c.parentNode) c.parentNode.removeChild(c);
          canvasRef.current = null;
        },
      });
      new CanvasLayer().addTo(map);

      // Road labels on top of heatmap
      L.tileLayer('https://{s}.basemaps.cartocdn.com/light_only_labels/{z}/{x}/{y}{r}.png', {
        subdomains: 'abcd', maxZoom: 19, pane: 'shadowPane',
        attribution: '',
      }).addTo(map);

      // Buildings from backend
      fetch(`${BACKEND_URL}/buildings`).then(r => r.json()).then(geojson => {
        L.geoJSON(geojson, {
          style: {
            fillColor: '#9aa0ae', fillOpacity: 0.75,
            color: '#7a8090', weight: 0.8
          },
        }).addTo(map);
      }).catch(() => { });

      map.on('moveend zoomend', redraw);
      map.on('click', e =>
        setStations(prev => [...prev, { id: Date.now(), lat: e.latlng.lat, lng: e.latlng.lng }])
      );
      map.on('mousemove', e => onMousePos({ lat: e.latlng.lat, lng: e.latlng.lng }));
    });

    return () => {
      leafletRef.current?.remove();
      leafletRef.current = null;
      initRef.current = false;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // Keep redraw listener current
  useEffect(() => {
    const map = leafletRef.current;
    if (!map) return;
    map.off('moveend zoomend');
    map.on('moveend zoomend', redraw);
  }, [redraw]);

  // ── Sync markers ──────────────────────────────────────────────────────────
  useEffect(() => {
    const map = leafletRef.current;
    if (!map) return;
    import('leaflet').then(mod => {
      const L = mod.default || mod;
      markersRef.current.forEach(m => map.removeLayer(m));
      markersRef.current = [];

      stations.forEach((st, i) => {
        const col = STATION_COLORS[i % STATION_COLORS.length];
        // Small clean dot — same style as USC sim
        const icon = L.divIcon({
          className: '',
          html: `<div style="
            width:14px;height:14px;border-radius:50%;
            background:${col.hex};
            border:2.5px solid white;
            box-shadow:0 1px 6px rgba(0,0,0,0.45);
          "></div>`,
          iconSize: [14, 14],
          iconAnchor: [7, 7],
        });

        const marker = L.marker([st.lat, st.lng], { icon, draggable: true })
          .addTo(map)
          .bindTooltip(
            `<div style="font-size:11px;font-weight:600">BS ${i + 1}</div>
             <div style="font-size:10px;color:#888">${st.lat.toFixed(5)}, ${st.lng.toFixed(5)}</div>`,
            { direction: 'top', offset: [0, -10], className: 'bs-tooltip' }
          );

        marker.on('dragend', e => {
          const p = e.target.getLatLng();
          setStations(prev => prev.map((s, j) =>
            j === i ? { ...s, lat: p.lat, lng: p.lng } : s
          ));
        });
        marker.on('contextmenu', () =>
          setStations(prev => prev.filter((_, j) => j !== i))
        );
        markersRef.current.push(marker);
      });
    });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [stations]);

  // Trigger backend whenever stations or applied params change
  useEffect(() => {
    fetchCoverage(stations, params);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [stations, params]);

  return (
    <div className="relative flex-1 overflow-hidden">
      <div ref={mapRef} style={{ width: '100%', height: '100%' }} />

      {/* Loading pill */}
      {loading && (
        <div className="absolute top-3 left-1/2 -translate-x-1/2 z-[1001]
          bg-[#1e2130]/90 border border-[#2e3347] text-white text-xs
          px-4 py-1.5 rounded-full flex items-center gap-2 shadow-lg pointer-events-none">
          <span className="w-2 h-2 rounded-full bg-[#bf5700] animate-pulse" />
          Computing coverage…
        </div>
      )}

      {/* Legend */}
      {stations.length > 0 && (
        <div className="absolute top-3 right-3 z-[1000]
          bg-white/95 border border-gray-200 shadow-md rounded-lg p-2.5 min-w-[120px]">
          <p className="text-[10px] text-gray-400 uppercase tracking-widest mb-1.5 font-semibold">
            Stations
          </p>
          {stations.map((_, i) => {
            const col = STATION_COLORS[i % STATION_COLORS.length];
            return (
              <div key={i} className="flex items-center gap-2 mb-1">
                <span className="w-3 h-3 rounded-full border-2 border-white shadow-sm flex-shrink-0"
                  style={{ background: col.hex }} />
                <span className="text-xs text-gray-700">BS {i + 1}</span>
              </div>
            );
          })}
          <div className="mt-2 pt-1.5 border-t border-gray-100">
            <div className="flex items-center gap-1 mb-0.5">
              <div className="w-12 h-2 rounded-full"
                style={{ background: 'linear-gradient(to right, rgba(245,150,60,0.3), rgba(245,150,60,0.95))' }} />
            </div>
            <p className="text-[9px] text-gray-400">weak → strong</p>
          </div>
        </div>
      )}

      {/* Empty hint */}
      {stations.length === 0 && (
        <div className="absolute bottom-10 left-1/2 -translate-x-1/2 z-[999] pointer-events-none">
          <div className="bg-white/85 backdrop-blur-sm border border-gray-200
            shadow rounded-full px-5 py-2 text-sm text-gray-500 whitespace-nowrap">
            Click on campus to place a base station
          </div>
        </div>
      )}
    </div>
  );
}

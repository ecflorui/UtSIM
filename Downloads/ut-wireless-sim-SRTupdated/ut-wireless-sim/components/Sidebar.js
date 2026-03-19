'use client';
import { useState } from 'react';

function Slider({ label, value, min, max, step, unit, onChange }) {
  return (
    <div className="mb-3">
      <div className="flex justify-between items-center mb-1">
        <span className="param-label">{label}</span>
        <div className="flex items-center gap-1">
          <input type="number" value={value} step={step} min={min} max={max}
            onChange={e => onChange(parseFloat(e.target.value))}
            className="param-value" />
          <span className="text-[10px] text-[#8892aa]">{unit}</span>
        </div>
      </div>
      <input type="range" min={min} max={max} step={step} value={value}
        onChange={e => onChange(parseFloat(e.target.value))} />
    </div>
  );
}

function StatRow({ label, value, sub }) {
  return (
    <div className="flex justify-between items-center py-1.5 border-b border-[#ffffff08]">
      <span className="text-xs text-[#8892aa]">{label}</span>
      <div className="text-right">
        <span className="stat-val">{value}</span>
        {sub && <span className="text-[10px] text-[#666] ml-1">{sub}</span>}
      </div>
    </div>
  );
}

// Coverage quality label
function qualityLabel(pct) {
  if (pct === 0)  return { text: '—',        color: '#666'    };
  if (pct < 20)   return { text: 'Poor',      color: '#ef4444' };
  if (pct < 40)   return { text: 'Fair',      color: '#f97316' };
  if (pct < 65)   return { text: 'Good',      color: '#eab308' };
  if (pct < 85)   return { text: 'Very Good', color: '#22c55e' };
  return            { text: 'Excellent',   color: '#10b981' };
}

export default function Sidebar({ params, setParams, onApply, stations, setStations, stats }) {
  const [activeTab, setActiveTab] = useState('params'); // 'params' | 'info'

  function update(k, v) { setParams(p => ({ ...p, [k]: v })); }

  // Estimate min SNR margin across placed stations (rough)
  const threshold = params.noiseFloor + params.snrThreshold;
  const ql = qualityLabel(stats.coverage);

  return (
    <div className="panel w-64 flex-shrink-0 flex flex-col" style={{ height: '100%' }}>

      {/* Header */}
      <div className="px-4 pt-4 pb-3 border-b border-[#2e3347]">
        <div className="text-[#bf5700] font-bold text-sm tracking-widest uppercase">UT Austin</div>
        <div className="text-[#8892aa] text-[11px] tracking-wide mt-0.5">Wireless Coverage Sim</div>
      </div>

      {/* Tabs */}
      <div className="flex border-b border-[#2e3347]">
        {['params', 'info'].map(t => (
          <button key={t} onClick={() => setActiveTab(t)}
            className={`flex-1 py-2 text-xs font-semibold uppercase tracking-wider transition-colors ${
              activeTab === t
                ? 'text-[#bf5700] border-b-2 border-[#bf5700]'
                : 'text-[#8892aa] hover:text-white'
            }`}>
            {t === 'params' ? 'Parameters' : 'Info'}
          </button>
        ))}
      </div>

      <div className="flex-1 overflow-y-auto px-4 py-4 space-y-5">

        {activeTab === 'params' && (<>

          {/* RF Parameters */}
          <div>
            <div className="section-title">Radio / PHY</div>
            <Slider label="TX Power"     value={params.txPower}      min={-10} max={36}   step={1}   unit="dBm" onChange={v => update('txPower', v)} />
            <Slider label="Noise Floor"  value={params.noiseFloor}   min={-120} max={-60} step={1}   unit="dBm" onChange={v => update('noiseFloor', v)} />
            <Slider label="SNR Threshold" value={params.snrThreshold} min={0}  max={30}   step={1}   unit="dB"  onChange={v => update('snrThreshold', v)} />
            <Slider label="Frequency"    value={params.freq}         min={0.9} max={6.0}  step={0.1} unit="GHz" onChange={v => update('freq', v)} />
          </div>

          {/* Sensitivity display */}
          <div className="rounded bg-[#111622] border border-[#2e3347] px-3 py-2 text-xs">
            <div className="flex justify-between text-[#8892aa] mb-1">
              <span>Coverage threshold</span>
              <span className="text-white font-mono">{threshold.toFixed(0)} dBm</span>
            </div>
            <div className="flex justify-between text-[#8892aa]">
              <span>Link budget</span>
              <span className="text-white font-mono">{(params.txPower - threshold).toFixed(0)} dB</span>
            </div>
          </div>

          <button className="apply-btn" onClick={onApply}>Apply Parameters</button>

          {/* Quick Presets */}
          <div>
            <div className="section-title">Standard Presets</div>
            <div className="space-y-1.5">
              {[
                { label: '2.4 GHz  802.11n',  freq: 2.4, txPower: 20, snrThreshold: 10 },
                { label: '5 GHz    802.11ac',  freq: 5.0, txPower: 17, snrThreshold: 15 },
                { label: '6 GHz    802.11ax',  freq: 6.0, txPower: 14, snrThreshold: 20 },
                { label: '900 MHz  IoT/LPWAN', freq: 0.9, txPower: 27, snrThreshold: 5  },
              ].map(p => (
                <button key={p.label} onClick={() =>
                  setParams(prev => ({ ...prev, freq: p.freq, txPower: p.txPower, snrThreshold: p.snrThreshold }))
                }
                  className="w-full text-left px-2.5 py-1.5 rounded bg-[#111622]
                    hover:bg-[#1a2030] text-[#8892aa] hover:text-white
                    border border-[#2e3347] text-xs transition-colors font-mono">
                  {p.label}
                </button>
              ))}
            </div>
          </div>

        </>)}

        {activeTab === 'info' && (<>

          {/* Coverage Stats */}
          <div>
            <div className="section-title">Coverage Statistics</div>
            <StatRow label="Base Stations"  value={stations.length} />
            <StatRow label="Area Covered"
              value={`${stats.coverage.toFixed(1)}%`}
              sub={<span style={{ color: ql.color }}>{ql.text}</span>} />
            <StatRow label="Max Range"      value={`${stats.maxRange.toFixed(0)} m`} />
            <StatRow label="Frequency"      value={`${params.freq.toFixed(1)} GHz`} />
            <StatRow label="TX Power"       value={`${params.txPower} dBm`} />
            <StatRow label="Threshold"      value={`${threshold.toFixed(0)} dBm`} />
          </div>

          {/* Coverage bar */}
          {stats.coverage > 0 && (
            <div>
              <div className="section-title">Coverage Map</div>
              <div className="h-2 rounded-full bg-[#2e3347] overflow-hidden">
                <div className="h-full rounded-full transition-all duration-500"
                  style={{
                    width: `${stats.coverage}%`,
                    background: `linear-gradient(to right, #ef4444, #eab308, #22c55e)`,
                  }} />
              </div>
              <div className="flex justify-between text-[10px] text-[#8892aa] mt-1">
                <span>0%</span><span style={{ color: ql.color }}>{ql.text}</span><span>100%</span>
              </div>
            </div>
          )}

          {/* Placed stations */}
          {stations.length > 0 && (
            <div>
              <div className="section-title">Placed Stations</div>
              <div className="space-y-1">
                {stations.map((st, i) => (
                  <div key={st.id} className="flex items-center justify-between
                    bg-[#111622] rounded px-2 py-1.5 text-xs border border-[#2e3347]">
                    <span className="text-[#bf5700] font-bold">BS {i+1}</span>
                    <span className="text-[#8892aa] font-mono text-[10px]">
                      {st.lat.toFixed(4)}, {st.lng.toFixed(4)}
                    </span>
                    <button onClick={() => setStations(prev => prev.filter((_,j) => j!==i))}
                      className="text-red-500 hover:text-red-400 ml-1 text-xs leading-none">✕</button>
                  </div>
                ))}
              </div>
              <button onClick={() => setStations([])}
                className="w-full mt-2 py-1.5 text-xs text-red-400 hover:text-red-300
                  border border-red-900/40 hover:border-red-700 rounded transition-colors">
                Clear All Stations
              </button>
            </div>
          )}

          {/* Physics note */}
          <div className="rounded bg-[#bf570010] border border-[#bf570030] p-3 text-xs">
            <div className="text-[#bf5700] font-semibold mb-1">📡 Physics Model</div>
            <div className="text-[#8892aa] space-y-1">
              <div>• Free-space path loss (FSPL)</div>
              <div>• ITU-R wall penetration: <span className="text-white">12 dB/wall</span></div>
              <div>• Log-normal shadowing: <span className="text-white">σ = 6 dB</span></div>
              <div>• Diffraction at building corners</div>
              <div className="pt-1 text-[#bf5700]">Upgrade: Sionna RT ray tracing (GPU)</div>
            </div>
          </div>

        </>)}
      </div>

      {/* Instructions footer */}
      <div className="px-4 py-3 border-t border-[#2e3347] text-[10px] text-[#666] space-y-0.5">
        <div><span className="text-[#8892aa]">Click</span> map → place station</div>
        <div><span className="text-[#8892aa]">Drag</span> station → move it</div>
        <div><span className="text-[#8892aa]">Right-click</span> station → delete</div>
      </div>
    </div>
  );
}

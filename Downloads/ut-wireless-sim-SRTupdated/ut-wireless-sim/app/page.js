'use client';
import { useState, useCallback } from 'react';
import Sidebar from '../components/Sidebar';
import CampusMap from '../components/CampusMap';

const DEFAULT_PARAMS = {
  txPower: 20,
  noiseFloor: -101,
  snrThreshold: 10,
  shadowingStd: 4,
  freq: 2.4,
};

export default function Home() {
  const [params, setParams]           = useState(DEFAULT_PARAMS);
  const [appliedParams, setApplied]   = useState(DEFAULT_PARAMS);
  const [stations, setStations]       = useState([]);
  const [stats, setStats]             = useState({ coverage: 0, maxRange: 0 });
  const [mousePos, setMousePos]       = useState({ lat: 0, lng: 0 });
  const [engine, setEngine]           = useState('—');

  const handleApply = useCallback(() => setApplied({ ...params }), [params]);

  return (
    <div className="flex flex-col" style={{ height: '100vh' }}>
      {/* Top bar */}
      <div className="flex items-center gap-3 px-4 py-2 bg-[#111622] border-b border-[#2e3347] z-10">
        <div className="flex items-center gap-2">
          {/* Longhorn icon */}
          <svg viewBox="0 0 32 32" width="22" height="22" fill="#bf5700">
            <path d="M16 3C9.37 3 4 8.37 4 15c0 4.5 2.3 8.5 5.8 10.8L8 29h3l1.5-2.8c1.1.4 2.3.6 3.5.6s2.4-.2 3.5-.6L21 29h3l-1.8-3.2C25.7 23.5 28 19.5 28 15 28 8.37 22.63 3 16 3zm0 2c5.52 0 10 4.48 10 10s-4.48 10-10 10S6 20.52 6 15 10.48 5 16 5z"/>
          </svg>
          <span className="text-[#bf5700] font-bold text-sm tracking-wider">UT AUSTIN</span>
        </div>
        <span className="text-[#8892aa] text-sm">Wireless Coverage Simulator</span>
        <div className="ml-auto flex items-center gap-4 text-xs">
          <span className="text-[#8892aa]">Engine:</span>
          <span className={`font-semibold px-2 py-0.5 rounded text-xs ${
            engine === 'sionna'   ? 'bg-green-900/40 text-green-400 border border-green-700' :
            engine === 'fallback' ? 'bg-yellow-900/40 text-yellow-400 border border-yellow-700' :
                                    'bg-[#1e2130] text-[#8892aa] border border-[#2e3347]'
          }`}>
            {engine === 'sionna' ? '⚡ Sionna RT' : engine === 'fallback' ? '⚠ Fallback' : engine}
          </span>
        </div>
      </div>

      {/* Main layout */}
      <div className="flex flex-1 overflow-hidden">
        <Sidebar
          params={params}
          setParams={setParams}
          onApply={handleApply}
          stations={stations}
          setStations={setStations}
          stats={stats}
        />
        <div className="flex flex-col flex-1 overflow-hidden">
          <CampusMap
            stations={stations}
            setStations={setStations}
            params={appliedParams}
            onStatsUpdate={setStats}
            onMousePos={setMousePos}
            onEngineUpdate={setEngine}
          />
          <div className="status-bar">
            <span>
              {stations.length === 0
                ? 'Click on campus to place base stations · Right-click a marker to remove it'
                : `${stations.length} station${stations.length > 1 ? 's' : ''} placed`}
            </span>
            <span>
              {mousePos.lat ? `${mousePos.lat.toFixed(5)}, ${mousePos.lng.toFixed(5)}` : ''}
            </span>
          </div>
        </div>
      </div>
    </div>
  );
}

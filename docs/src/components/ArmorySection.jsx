import React, { useState } from 'react';
import { CHARACTERS } from '../data/gameData';
import ChartComponent from './ChartComponent';

// 맵 이미지 import
import mapImage from '../assets/maps/map.png';
import map2Image from '../assets/maps/map2.png';
import map3Image from '../assets/maps/map3.png';
import villageImage from '../assets/maps/village.png';

// 캐릭터 스프라이트 import
import ravenSprite from '../assets/characters/Raven_48_64.png';
import piperSprite from '../assets/characters/Piper_48_64.png';
import generalSprite from '../assets/characters/General_48_64.png';
import technicianSprite from '../assets/characters/Technician_48_64.png';
import wildcatSprite from '../assets/characters/wildcat_48_64.png';
import sageSprite from '../assets/characters/Sage.png';

const CHARACTER_SPRITES = {
  raven: ravenSprite,
  piper: piperSprite,
  general: generalSprite,
  technician: technicianSprite,
  wildcat: wildcatSprite,
  sage: sageSprite
};

// 맵 데이터
const MAPS = {
  map: {
    name: 'Classic Arena',
    image: mapImage,
    size: '1280x720',
    description: '대칭 구조의 기본 맵. 중앙 광장과 좌우 복도로 구성되어 있으며, 균형잡힌 전투를 제공합니다.',
    features: ['중앙 광장 교전', '좌우 대칭 구조', '초보자 친화적', 'Red/Blue 스폰 균형']
  },
  map2: {
    name: 'Industrial Zone',
    image: map2Image,
    size: '1280x720',
    description: '공장 지대를 모티브로 한 맵. 복잡한 통로와 엄폐물이 많아 전술적 플레이가 가능합니다.',
    features: ['다수의 엄폐물', '복잡한 동선', '매복 전략 유리', '좁은 통로 교전']
  },
  map3: {
    name: 'Desert Outpost',
    image: map3Image,
    size: '1280x720',
    description: '사막 전초기지 테마. 넓은 공간과 장애물이 조화를 이루며, 장거리 저격과 근접전이 공존합니다.',
    features: ['개활지 중심', '전략적 장애물 배치', '저격 포지션', '빠른 템포']
  },
  village: {
    name: 'Abandoned Village',
    image: villageImage,
    size: '1280x720',
    description: '버려진 마을 맵. 건물 사이를 오가며 도심전을 즐길 수 있으며, 수직 구조가 특징입니다.',
    features: ['건물 밀집 지역', '다양한 고저차', '좁은 골목길', '어두운 분위기']
  }
};

const ArmorySection = () => {
  const [selectedCharKey, setSelectedCharKey] = useState('raven');
  const [selectedMapKey, setSelectedMapKey] = useState('map');
  const [activeTab, setActiveTab] = useState('characters'); // 'characters' | 'maps'
  
  const character = CHARACTERS[selectedCharKey];
  const map = MAPS[selectedMapKey];

  const radarData = {
    labels: ['Damage', 'Range', 'Speed', 'Utility'],
    datasets: [{
      label: 'Stats',
      data: character.stats,
      backgroundColor: 'rgba(59, 130, 246, 0.2)',
      borderColor: '#3b82f6',
      pointBackgroundColor: '#3b82f6',
      pointBorderColor: '#fff',
      pointHoverBackgroundColor: '#fff',
      pointHoverBorderColor: '#3b82f6'
    }]
  };

  return (
    <div className="space-y-8 animate-fade-in">
      <div className="bg-white rounded-xl shadow-sm p-6 border border-slate-100">
        <h2 className="text-2xl font-bold text-slate-800 mb-2">캐릭터 & 맵 분석 (Armory)</h2>
        <p className="text-slate-600">6종의 캐릭터와 4개의 맵을 비교하고 전략을 수립하세요.</p>
      </div>

      {/* 탭 전환 버튼 */}
      <div className="flex gap-4 border-b-2 border-slate-200">
        <button
          onClick={() => setActiveTab('characters')}
          className={`px-6 py-3 font-bold text-lg transition-all ${
            activeTab === 'characters'
              ? 'text-blue-600 border-b-4 border-blue-600 -mb-0.5'
              : 'text-slate-400 hover:text-slate-600'
          }`}
        >
          🎯 캐릭터
        </button>
        <button
          onClick={() => setActiveTab('maps')}
          className={`px-6 py-3 font-bold text-lg transition-all ${
            activeTab === 'maps'
              ? 'text-green-600 border-b-4 border-green-600 -mb-0.5'
              : 'text-slate-400 hover:text-slate-600'
          }`}
        >
          🗺️ 맵
        </button>
      </div>

      {/* 캐릭터 섹션 */}
      {activeTab === 'characters' && (
        <div className="grid grid-cols-1 md:grid-cols-12 gap-8">
          <div className="md:col-span-3 space-y-2">
            <label className="block text-xs font-bold text-slate-400 uppercase mb-2">Select Class</label>
            <div className="space-y-1">
              {Object.keys(CHARACTERS).map(key => (
                <button
                  key={key}
                  onClick={() => setSelectedCharKey(key)}
                  className={`w-full text-left px-4 py-3 rounded border transition-colors text-sm font-medium flex justify-between items-center ${
                    selectedCharKey === key 
                      ? 'bg-blue-50 border-blue-300 text-blue-700' 
                      : 'bg-white border-slate-200 text-slate-700 hover:bg-slate-50'
                  }`}
                >
                  {CHARACTERS[key].name}
                </button>
              ))}
            </div>
          </div>

          {/* 캐릭터 이미지 */}
          <div className="md:col-span-3 bg-white p-6 rounded-xl shadow-sm border border-slate-100 flex flex-col items-center justify-center">
            <img 
              src={CHARACTER_SPRITES[character.id]}
              alt={character.name}
              className="w-48 h-64 object-contain image-rendering-pixelated mb-4"
              style={{ imageRendering: 'pixelated' }}
            />
            <span className="text-sm font-bold text-slate-500">{character.id}_48_64.png</span>
            <span className="text-xs text-slate-400 mt-1">48x64 Sprite Sheet</span>
          </div>

          <div className="md:col-span-4 bg-white p-6 rounded-xl shadow-sm border border-slate-100">
            <div className="flex justify-between items-start mb-4">
              <h3 className="text-2xl font-bold text-slate-800">{character.name}</h3>
              <span className="px-2 py-1 bg-blue-100 text-blue-700 text-xs font-bold rounded">{character.role}</span>
            </div>
            
            <div className="space-y-4">
              <div>
                <div className="text-xs font-bold text-slate-400 uppercase">Skills</div>
                <ul className="mt-2 space-y-3">
                  {character.skills.map((skill, i) => (
                    <li key={i} className="text-sm text-slate-700 bg-slate-50 p-2 rounded border border-slate-100">
                      {skill}
                    </li>
                  ))}
                </ul>
              </div>
              <div className="p-3 bg-slate-50 rounded border-l-4 border-slate-300 text-sm text-slate-600 italic">
                {character.desc}
              </div>
            </div>
          </div>

          <div className="md:col-span-2 bg-white p-6 rounded-xl shadow-sm border border-slate-100 flex flex-col items-center justify-center">
            <h4 className="text-sm font-bold text-slate-500 mb-2">Performance Profile</h4>
            <ChartComponent 
              type="radar" 
              data={radarData} 
              options={{
                scales: {
                  r: { angleLines: { display: false }, suggestedMin: 0, suggestedMax: 100 }
                },
                plugins: { legend: { display: false } }
              }} 
            />
          </div>
        </div>
      )}

      {/* 맵 섹션 */}
      {activeTab === 'maps' && (
        <div className="grid grid-cols-1 md:grid-cols-12 gap-8">
          <div className="md:col-span-3 space-y-2">
            <label className="block text-xs font-bold text-slate-400 uppercase mb-2">Select Map</label>
            <div className="space-y-1">
              {Object.keys(MAPS).map(key => (
                <button
                  key={key}
                  onClick={() => setSelectedMapKey(key)}
                  className={`w-full text-left px-4 py-3 rounded border transition-colors text-sm font-medium ${
                    selectedMapKey === key 
                      ? 'bg-green-50 border-green-300 text-green-700' 
                      : 'bg-white border-slate-200 text-slate-700 hover:bg-slate-50'
                  }`}
                >
                  {MAPS[key].name}
                </button>
              ))}
            </div>
          </div>

          {/* 맵 이미지 */}
          <div className="md:col-span-6 bg-white p-6 rounded-xl shadow-sm border border-slate-100">
            <h3 className="text-2xl font-bold text-slate-800 mb-4">{map.name}</h3>
            <img 
              src={map.image}
              alt={map.name}
              className="w-full rounded-lg border-2 border-slate-200 mb-4"
            />
            <div className="flex gap-2 mb-4">
              <span className="px-3 py-1 bg-slate-100 text-slate-600 text-xs font-bold rounded">
                📐 {map.size}
              </span>
              <span className="px-3 py-1 bg-green-100 text-green-700 text-xs font-bold rounded">
                🎮 {selectedMapKey}.png
              </span>
            </div>
          </div>

          {/* 맵 상세 정보 */}
          <div className="md:col-span-3 bg-white p-6 rounded-xl shadow-sm border border-slate-100">
            <h4 className="text-sm font-bold text-slate-400 uppercase mb-3">Map Features</h4>
            <p className="text-sm text-slate-600 mb-4 leading-relaxed">
              {map.description}
            </p>
            <div className="space-y-2">
              {map.features.map((feature, i) => (
                <div key={i} className="flex items-start gap-2 text-sm">
                  <span className="text-green-500 mt-0.5">✓</span>
                  <span className="text-slate-700">{feature}</span>
                </div>
              ))}
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default ArmorySection;


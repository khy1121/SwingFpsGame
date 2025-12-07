import React, { useState } from 'react';
import { ARCHITECTURE_DETAILS } from '../data/gameData';

const ArchitectureSection = () => {
  const [selectedDetail, setSelectedDetail] = useState(null);

  const handleSelect = (key) => {
    setSelectedDetail(ARCHITECTURE_DETAILS[key]);
  };

  return (
    <div className="space-y-8 animate-fade-in">
      <div className="bg-white rounded-xl shadow-sm p-6 border border-slate-100">
        <h2 className="text-2xl font-bold text-slate-800 mb-2">시스템 아키텍처 분석</h2>
        <p className="text-slate-600">
          NetFps는 서버-클라이언트 모델을 따르며, 클라이언트는 MVC 패턴으로 구조화되어 있습니다. 
          아래 다이어그램의 각 요소를 클릭하여 상세 역할을 확인하세요.
        </p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        <div className="lg:col-span-2 bg-white p-6 rounded-xl shadow-sm border border-slate-100">
          <h3 className="text-lg font-bold text-slate-800 mb-6 text-center">MVC & Manager Pattern Structure</h3>
          
          <div className="grid grid-cols-3 gap-4 text-center">
            {[
              { key: 'view', label: 'View', sub: 'GameRenderer', color: 'emerald' },
              { key: 'controller', label: 'Controller', sub: 'GamePanel / Network', color: 'amber' },
              { key: 'model', label: 'Model', sub: 'GameState', color: 'blue' }
            ].map(item => (
              <div 
                key={item.key}
                onClick={() => handleSelect(item.key)}
                className={`p-4 rounded-lg bg-${item.color}-50 border-2 border-${item.color}-200 cursor-pointer hover:bg-${item.color}-100 transition`}
              >
                <div className={`font-bold text-${item.color}-800`}>{item.label}</div>
                <div className={`text-sm text-${item.color}-600`}>{item.sub}</div>
              </div>
            ))}
          </div>

          <div className="my-6 border-t border-slate-200"></div>

          <div className="text-center mb-2 font-semibold text-slate-500 text-sm">MANAGERS (Phase 2 Refactoring - 8 Classes)</div>
          <div className="grid grid-cols-2 md:grid-cols-4 gap-2">
            {['manager_map', 'manager_skill', 'manager_ui', 'manager_col', 'manager_obj', 'manager_logic', 'manager_movement', 'manager_spawn'].map(key => (
              <button 
                key={key}
                onClick={() => handleSelect(key)}
                className="py-2 px-3 bg-slate-100 rounded text-xs font-medium hover:bg-slate-200 capitalize"
              >
                {ARCHITECTURE_DETAILS[key].title}
              </button>
            ))}
          </div>
        </div>

        <div className="bg-slate-50 p-6 rounded-xl border border-slate-200">
          <h4 className="text-lg font-bold text-slate-800 mb-2">
            {selectedDetail ? selectedDetail.title : "요소 선택"}
          </h4>
          <p className="text-sm text-slate-600 leading-relaxed min-h-[100px]">
            {selectedDetail 
              ? selectedDetail.desc 
              : "왼쪽 다이어그램에서 구성 요소를 클릭하면 상세 설명이 이곳에 표시됩니다."}
          </p>
          {selectedDetail && (
            <div className="mt-4 p-3 bg-slate-800 rounded text-xs text-green-400 font-mono overflow-x-auto">
              {selectedDetail.code}
            </div>
          )}
        </div>
      </div>

      <div className="bg-white p-6 rounded-xl shadow-sm border border-slate-100">
        <h3 className="text-lg font-bold text-slate-800 mb-4">Game Loop (60 FPS / 16ms)</h3>
        <div className="flex flex-col md:flex-row justify-between items-center gap-4">
          {[
            { step: 'Step 1', title: 'Update Logic', sub: 'Player Pos, Missiles' },
            { step: 'Step 2', title: 'Check Physics', sub: 'Collisions (200ms CD)' },
            { step: 'Step 3', title: 'Render Screen', sub: 'Repaint Graphics' }
          ].map((step, i) => (
            <React.Fragment key={i}>
              <div className="flex-1 text-center p-4 bg-slate-50 rounded-lg w-full">
                <div className="text-xs font-bold text-slate-400 uppercase mb-1">{step.step}</div>
                <div className="font-bold text-slate-700">{step.title}</div>
                <div className="text-xs text-slate-500">{step.sub}</div>
              </div>
              {i < 2 && <div className="text-slate-300 transform rotate-90 md:rotate-0">➜</div>}
            </React.Fragment>
          ))}
        </div>
      </div>
    </div>
  );
};

export default ArchitectureSection;

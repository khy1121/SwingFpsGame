import React from 'react';

const DevToolsSection = () => (
  <div className="space-y-8 animate-fade-in">
    <div className="bg-white rounded-xl shadow-sm p-6 border border-slate-100">
      <h2 className="text-2xl font-bold text-slate-800 mb-2">개발 도구 & 유지보수</h2>
      <p className="text-slate-600">다양한 개발 도구를 활용하여 생산성을 극대화했습니다.</p>
    </div>

    <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
      {/* Development Tools */}
      <div className="bg-white p-6 rounded-xl shadow-sm border border-slate-100">
        <h3 className="text-lg font-bold text-slate-800 mb-4">Development Tools</h3>
        <div className="space-y-3">
          {[
            { icon: '🗺️', name: 'Tiled Map Editor', desc: 'JSON 기반 맵 데이터 편집' },
            { icon: '🎨', name: 'Piskel', desc: '48x64px 캐릭터 스프라이트 제작' },
            { icon: '💻', name: 'VS Code', desc: 'React 문서 사이트 개발' },
            { icon: '☕', name: 'Eclipse IDE', desc: 'Java 게임 클라이언트/서버 개발' },
            { icon: '🤖', name: 'Claude Sonnet 4.5', desc: '코드 리뷰 및 리팩토링 참여' },
            { icon: '🔧', name: 'Git/GitHub', desc: '버전 관리 및 협업' }
          ].map((tool, i) => (
            <div key={i} className="flex items-center p-3 bg-slate-50 rounded border border-slate-100">
              <span className="text-2xl mr-3">{tool.icon}</span>
              <div className="flex-1">
                <div className="font-bold text-slate-800 text-sm">{tool.name}</div>
                <div className="text-xs text-slate-500">{tool.desc}</div>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* In-game Editor Keys */}
      <div className="bg-white p-6 rounded-xl shadow-sm border border-slate-100">
        <h3 className="text-lg font-bold text-slate-800 mb-4">In-game Editor (Function Keys)</h3>
        <div className="grid grid-cols-2 gap-4 mb-4">
          {[
            { key: 'F3', title: 'Debug Grid', desc: '장애물 타일 시각화' },
            { key: 'F4', title: 'Edit Mode', desc: '맵 편집기 활성화' },
            { key: 'F5', title: 'Save Map', desc: 'JSON 파일로 저장' },
            { key: 'F6', title: 'Load Map', desc: '다른 맵으로 전환' }
          ].map((item, i) => (
            <div key={i} className="flex items-center p-3 bg-slate-50 rounded border border-slate-200">
              <kbd className="px-2 py-1 bg-white border border-slate-300 rounded text-sm font-bold text-slate-700 shadow-sm mr-3">{item.key}</kbd>
              <div>
                <div className="font-bold text-slate-800 text-sm">{item.title}</div>
                <div className="text-xs text-slate-500">{item.desc}</div>
              </div>
            </div>
          ))}
        </div>
        
        {/* Paint Mode Keys */}
        <div className="mt-4 p-3 bg-blue-50 rounded border border-blue-100">
          <div className="text-xs font-bold text-blue-700 mb-2">EDIT MODE PAINT (1-4 Keys)</div>
          <div className="grid grid-cols-2 gap-2 text-xs">
            <div className="flex items-center">
              <kbd className="px-2 py-1 bg-white border border-slate-300 rounded text-xs mr-2">1</kbd>
              <span className="text-slate-600">이동 가능</span>
            </div>
            <div className="flex items-center">
              <kbd className="px-2 py-1 bg-white border border-slate-300 rounded text-xs mr-2">2</kbd>
              <span className="text-slate-600">장애물</span>
            </div>
            <div className="flex items-center">
              <kbd className="px-2 py-1 bg-white border border-slate-300 rounded text-xs mr-2">3</kbd>
              <span className="text-red-600">RED 스폰</span>
            </div>
            <div className="flex items-center">
              <kbd className="px-2 py-1 bg-white border border-slate-300 rounded text-xs mr-2">4</kbd>
              <span className="text-blue-600">BLUE 스폰</span>
            </div>
          </div>
        </div>
      </div>

      {/* Future Roadmap */}
      <div className="bg-white p-6 rounded-xl shadow-sm border border-slate-100 md:col-span-2">
        <h3 className="text-lg font-bold text-slate-800 mb-4">Future Roadmap</h3>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
          {[
            { color: 'bg-blue-500', text: 'UDP 프로토콜 도입 (위치 동기화 최적화)' },
            { color: 'bg-blue-500', text: 'Dedicated Server (클라우드 배포)' },
            { color: 'bg-blue-500', text: 'Replay System (경기 녹화 및 재생)' },
            { color: 'bg-blue-500', text: '관전 모드 (Observer)' },
            { color: 'bg-slate-300', text: 'Undo/Redo 기능 (맵 에디터)' },
            { color: 'bg-slate-300', text: '브러시 크기 선택 (1x1, 2x2, 3x3)' }
          ].map((item, i) => (
            <li key={i} className="flex items-center text-slate-600 text-sm">
              <span className={`w-2 h-2 ${item.color} rounded-full mr-3`}></span>
              {item.text}
            </li>
          ))}
        </div>
      </div>
    </div>
  </div>
);

export default DevToolsSection;

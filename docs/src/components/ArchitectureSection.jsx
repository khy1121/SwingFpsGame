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
        <div className="flex flex-col md:flex-row justify-between items-center gap-4 mb-6">
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
        
        {/* Detailed Game Loop Explanation */}
        <div className="bg-slate-50 p-4 rounded-lg border border-slate-200">
          <div className="text-sm text-slate-700 space-y-2">
            <div className="font-bold text-slate-800 mb-2">⏱️ 게임 루프 상세</div>
            <div className="flex items-start gap-2">
              <span className="text-blue-600">•</span>
              <span><strong>javax.swing.Timer</strong>로 16ms마다 실행 (1000ms/60 = 16.67ms ≈ 60 FPS)</span>
            </div>
            <div className="flex items-start gap-2">
              <span className="text-blue-600">•</span>
              <span><strong>EDT(Event Dispatch Thread)</strong>에서 실행되어 Swing 컴포넌트와 안전하게 상호작용</span>
            </div>
            <div className="flex items-start gap-2">
              <span className="text-blue-600">•</span>
              <span><strong>updateGame()</strong>: 플레이어 이동, 미사일 업데이트, 충돌 감지, 스킬 쿨다운</span>
            </div>
            <div className="flex items-start gap-2">
              <span className="text-blue-600">•</span>
              <span><strong>repaint()</strong>: GameRenderer가 RenderContext 기반으로 화면 그리기</span>
            </div>
            <div className="flex items-start gap-2">
              <span className="text-blue-600">•</span>
              <span><strong>네트워크 스레드</strong>: 별도 스레드에서 서버 메시지 수신 (논블로킹)</span>
            </div>
          </div>
        </div>
      </div>

      {/* Refactoring Journey */}
      <div className="bg-gradient-to-r from-purple-600 to-pink-600 rounded-xl shadow-lg p-6 text-white">
        <h3 className="text-xl font-bold mb-4">🔄 리팩토링 여정</h3>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          <div className="bg-white/10 p-4 rounded-lg">
            <div className="text-2xl font-bold mb-2">Before</div>
            <div className="text-purple-100 text-sm space-y-1">
              <div>• GamePanel: 모놀리식</div>
              <div>• God Object 패턴</div>
              <div>• 렌더링과 로직 혼재</div>
              <div>• 중복 필드 다수</div>
            </div>
          </div>
          <div className="bg-white/10 p-4 rounded-lg">
            <div className="text-2xl font-bold mb-2">Phase 1 & 2</div>
            <div className="text-purple-100 text-sm space-y-1">
              <div>• GameRenderer 추출</div>
              <div>• 8개 Manager 분리</div>
              <div>• 상태 중앙 관리</div>
              <div>• 동기화 코드 제거</div>
            </div>
          </div>
          <div className="bg-white/10 p-4 rounded-lg">
            <div className="text-2xl font-bold mb-2">After</div>
            <div className="text-purple-100 text-sm space-y-1">
              <div>• GamePanel: 2,413줄 (현재)</div>
              <div>• MVC + Manager 패턴</div>
              <div>• SOLID 원칙 준수</div>
              <div>• 코드 분리 및 정리</div>
            </div>
          </div>
        </div>
      </div>

      {/* Server Architecture */}
      <div className="bg-white p-6 rounded-xl shadow-sm border border-slate-100">
        <h3 className="text-lg font-bold text-slate-800 mb-4">🖥️ 서버 아키텍처 (GameServer)</h3>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          <div>
            <div className="font-bold text-slate-700 mb-3">멀티스레드 구조</div>
            <div className="space-y-2 text-sm text-slate-600">
              <div className="bg-purple-50 p-3 rounded border border-purple-200">
                <div className="font-bold text-purple-900">메인 스레드</div>
                <div className="text-xs text-purple-700 mt-1">
                  ServerSocket.accept()로 클라이언트 연결 대기
                </div>
              </div>
              <div className="bg-purple-50 p-3 rounded border border-purple-200">
                <div className="font-bold text-purple-900">ClientHandler 스레드 (4개)</div>
                <div className="text-xs text-purple-700 mt-1">
                  각 클라이언트마다 독립 스레드로 메시지 처리
                </div>
              </div>
              <div className="bg-purple-50 p-3 rounded border border-purple-200">
                <div className="font-bold text-purple-900">6개 ConcurrentHashMap</div>
                <div className="text-xs text-purple-700 mt-1">
                  clients (플레이어), placedObjects (지뢰/터렛), activeAuras (버프), scheduledStrikes (공습), playerCharacterChanged (캐릭터 변경 추적) - 멀티스레드 안전 보장
                </div>
              </div>
            </div>
          </div>
          <div>
            <div className="font-bold text-slate-700 mb-3">권위 서버 모델</div>
            <div className="space-y-2 text-sm text-slate-600">
              <div className="bg-indigo-50 p-3 rounded border border-indigo-200">
                <div className="font-bold text-indigo-900">피해 판정</div>
                <div className="text-xs text-indigo-700 mt-1">
                  클라이언트가 HIT/HITME 보고 → 서버가 최종 검증
                </div>
              </div>
              <div className="bg-indigo-50 p-3 rounded border border-indigo-200">
                <div className="font-bold text-indigo-900">스킬 검증</div>
                <div className="text-xs text-indigo-700 mt-1">
                  쿨다운, 라운드 시간 제한 등 서버가 체크
                </div>
              </div>
              <div className="bg-indigo-50 p-3 rounded border border-indigo-200">
                <div className="font-bold text-indigo-900">브로드캐스트</div>
                <div className="text-xs text-indigo-700 mt-1">
                  검증된 이벤트만 모든 클라이언트에 전파
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default ArchitectureSection;

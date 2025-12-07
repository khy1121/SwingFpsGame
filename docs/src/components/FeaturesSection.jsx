import React from 'react';

const FeaturesSection = () => {
  return (
    <div className="space-y-8 animate-fade-in">
      <div className="bg-white rounded-xl shadow-sm p-6 border border-slate-100">
        <h2 className="text-2xl font-bold text-slate-800 mb-2">주요 기능 (Features)</h2>
        <p className="text-slate-600">
          NetFps의 독자적인 게임플레이 기능들을 소개합니다. 
          실시간 스코어보드, 맵 에디터, 그리고 다양한 전투 시스템을 확인하세요.
        </p>
      </div>

      {/* 스코어보드 기능 */}
      <div className="bg-white rounded-xl shadow-sm p-6 border border-slate-100">
        <h3 className="text-xl font-bold text-slate-800 mb-4 flex items-center gap-2">
          <span className="bg-blue-100 text-blue-700 px-3 py-1 rounded text-sm font-bold">TAB</span>
          실시간 스코어보드
        </h3>
        
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          <div>
            <h4 className="font-bold text-slate-700 mb-3">핵심 기능</h4>
            <ul className="space-y-2 text-sm text-slate-600">
              <li className="flex items-start">
                <span className="text-blue-600 mr-2">•</span>
                <span><strong>TAB 키 토글:</strong> 키를 누르는 동안만 표시되는 논-블로킹 UI</span>
              </li>
              <li className="flex items-start">
                <span className="text-blue-600 mr-2">•</span>
                <span><strong>실시간 통계:</strong> 킬/데스, K/D 비율, 캐릭터 정보</span>
              </li>
              <li className="flex items-start">
                <span className="text-blue-600 mr-2">•</span>
                <span><strong>팀별 분류:</strong> RED/BLUE 팀으로 구분하여 색상 코딩</span>
              </li>
              <li className="flex items-start">
                <span className="text-blue-600 mr-2">•</span>
                <span><strong>K/D 정렬:</strong> 성적 순으로 자동 정렬</span>
              </li>
              <li className="flex items-start">
                <span className="text-blue-600 mr-2">•</span>
                <span><strong>궁극기 상태:</strong> 팀원의 스킬 쿨다운 정보 표시</span>
              </li>
              <li className="flex items-start">
                <span className="text-blue-600 mr-2">•</span>
                <span><strong>사망 후에도 확인 가능:</strong> 관전 중 팀 상황 파악</span>
              </li>
            </ul>
          </div>

          <div className="bg-slate-800 p-4 rounded-lg text-slate-100 font-mono text-xs">
            <div className="text-center mb-3 text-lg font-bold text-white">SCOREBOARD</div>
            <div className="text-center mb-3 text-sm">RED 1 : 0 BLUE</div>
            <div className="border-t border-slate-600 pt-2 mb-2">
              <div className="grid grid-cols-6 gap-2 text-slate-400 mb-1">
                <span>NAME</span>
                <span>CHARACTER</span>
                <span>ULT</span>
                <span>K</span>
                <span>D</span>
                <span>K/D</span>
              </div>
            </div>
            <div className="mb-3">
              <div className="text-red-400 font-bold mb-1">RED TEAM</div>
              <div className="grid grid-cols-6 gap-2 text-xs bg-yellow-900 bg-opacity-30 p-1 rounded">
                <span>Alice</span>
                <span>Raven</span>
                <span className="text-green-400">READY</span>
                <span>5</span>
                <span>2</span>
                <span>2.50</span>
              </div>
              <div className="grid grid-cols-6 gap-2 text-xs mt-1">
                <span>Bob</span>
                <span>Piper</span>
                <span className="text-slate-400">3.5s</span>
                <span>3</span>
                <span>3</span>
                <span>1.00</span>
              </div>
            </div>
            <div>
              <div className="text-blue-400 font-bold mb-1">BLUE TEAM</div>
              <div className="grid grid-cols-6 gap-2 text-xs">
                <span>Charlie</span>
                <span>General</span>
                <span className="text-green-400">READY</span>
                <span>4</span>
                <span>1</span>
                <span>4.00</span>
              </div>
              <div className="grid grid-cols-6 gap-2 text-xs mt-1">
                <span>Dave</span>
                <span>Tech</span>
                <span className="text-slate-400">10.2s</span>
                <span>2</span>
                <span>4</span>
                <span>0.50</span>
              </div>
            </div>
            <div className="text-center mt-3 text-slate-500 text-xs">TAB 키를 떼면 닫힙니다</div>
          </div>
        </div>
      </div>

      {/* 맵 에디터 */}
      <div className="bg-white rounded-xl shadow-sm p-6 border border-slate-100">
        <h3 className="text-xl font-bold text-slate-800 mb-4">인게임 맵 에디터</h3>
        
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          <div className="bg-slate-50 p-4 rounded-lg border border-slate-200">
            <div className="flex items-center gap-2 mb-2">
              <kbd className="px-2 py-1 bg-white border border-slate-300 rounded text-sm font-bold">F3</kbd>
              <span className="font-bold text-slate-700">Debug Grid</span>
            </div>
            <p className="text-sm text-slate-600">장애물 타일을 시각화하여 충돌 영역을 확인합니다.</p>
          </div>

          <div className="bg-slate-50 p-4 rounded-lg border border-slate-200">
            <div className="flex items-center gap-2 mb-2">
              <kbd className="px-2 py-1 bg-white border border-slate-300 rounded text-sm font-bold">F4</kbd>
              <span className="font-bold text-slate-700">Edit Mode</span>
            </div>
            <p className="text-sm text-slate-600">실시간으로 맵을 편집할 수 있는 에디터 모드를 활성화합니다.</p>
          </div>

          <div className="bg-slate-50 p-4 rounded-lg border border-slate-200">
            <div className="flex items-center gap-2 mb-2">
              <kbd className="px-2 py-1 bg-white border border-slate-300 rounded text-sm font-bold">F5</kbd>
              <span className="font-bold text-slate-700">Save Map</span>
            </div>
            <p className="text-sm text-slate-600">현재 맵 데이터를 JSON 파일로 저장합니다.</p>
          </div>
        </div>

        <div className="mt-4 p-4 bg-blue-50 rounded-lg border-l-4 border-blue-500">
          <p className="text-sm text-slate-700">
            <strong>개발자 친화적:</strong> 게임을 종료하지 않고 맵을 수정하고 테스트할 수 있어 
            개발 및 밸런스 조정 시간을 크게 단축시킵니다.
          </p>
        </div>
      </div>

      {/* 캐릭터 스킬 시스템 */}
      <div className="bg-white rounded-xl shadow-sm p-6 border border-slate-100">
        <h3 className="text-xl font-bold text-slate-800 mb-4">캐릭터 스킬 시스템 (4개 캐릭터 활성화)</h3>
        
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          {/* Raven */}
          <div className="bg-gradient-to-br from-red-50 to-orange-50 p-5 rounded-lg border-2 border-red-200">
            <div className="flex items-center gap-3 mb-3">
              <div className="h-12 w-12 bg-red-600 rounded-full flex items-center justify-center text-white font-bold text-xl">R</div>
              <div>
                <h4 className="font-bold text-slate-800 text-lg">Raven</h4>
                <p className="text-xs text-slate-600">공격형 - 빠른 기동성과 높은 화력</p>
              </div>
            </div>
            <div className="space-y-2 text-sm">
              <div className="bg-white p-3 rounded border border-red-200">
                <div className="flex justify-between items-center mb-1">
                  <span className="font-bold text-red-700">E: 대쉬</span>
                  <span className="text-xs bg-red-100 px-2 py-1 rounded">5초 쿨다운</span>
                </div>
                <p className="text-slate-600 text-xs">벽을 통과하지 않는 순간이동 (200px). 0.5초 지속</p>
                <pre className="mt-2 bg-slate-800 p-2 rounded text-[10px] text-green-400 overflow-x-auto">ravenDashRemaining = 0.5f;</pre>
              </div>
              <div className="bg-white p-3 rounded border border-red-200">
                <div className="flex justify-between items-center mb-1">
                  <span className="font-bold text-red-700">R: 과충전</span>
                  <span className="text-xs bg-red-100 px-2 py-1 rounded">20초 쿨다운</span>
                </div>
                <p className="text-slate-600 text-xs">공격속도 35% 증가, 투사체 속도 1.8배. 6초 지속</p>
                <pre className="mt-2 bg-slate-800 p-2 rounded text-[10px] text-green-400 overflow-x-auto">missileSpeedMultiplier = 1.8f;</pre>
              </div>
            </div>
          </div>

          {/* Piper */}
          <div className="bg-gradient-to-br from-blue-50 to-purple-50 p-5 rounded-lg border-2 border-blue-200">
            <div className="flex items-center gap-3 mb-3">
              <div className="h-12 w-12 bg-blue-600 rounded-full flex items-center justify-center text-white font-bold text-xl">P</div>
              <div>
                <h4 className="font-bold text-slate-800 text-lg">Piper</h4>
                <p className="text-xs text-slate-600">정찰형 - 장거리 스나이퍼</p>
              </div>
            </div>
            <div className="space-y-2 text-sm">
              <div className="bg-white p-3 rounded border border-blue-200">
                <div className="flex justify-between items-center mb-1">
                  <span className="font-bold text-blue-700">E: 적 표시 (Mark)</span>
                  <span className="text-xs bg-blue-100 px-2 py-1 rounded">8초 쿨다운</span>
                </div>
                <p className="text-slate-600 text-xs">시야 범위 1.5배 증가 (팀 공유). 5초 지속</p>
                <pre className="mt-2 bg-slate-800 p-2 rounded text-[10px] text-green-400 overflow-x-auto">piperMarkRemaining = 5f;</pre>
              </div>
              <div className="bg-white p-3 rounded border border-blue-200">
                <div className="flex justify-between items-center mb-1">
                  <span className="font-bold text-blue-700">R: 열감지 (Thermal)</span>
                  <span className="text-xs bg-blue-100 px-2 py-1 rounded">30초 쿨다운</span>
                </div>
                <p className="text-slate-600 text-xs">모든 적 위치 미니맵 표시 (팀 공유). 8초 지속</p>
                <pre className="mt-2 bg-slate-800 p-2 rounded text-[10px] text-green-400 overflow-x-auto">piperThermalRemaining = 8f;</pre>
              </div>
            </div>
          </div>

          {/* Technician */}
          <div className="bg-gradient-to-br from-green-50 to-emerald-50 p-5 rounded-lg border-2 border-green-200">
            <div className="flex items-center gap-3 mb-3">
              <div className="h-12 w-12 bg-green-600 rounded-full flex items-center justify-center text-white font-bold text-xl">T</div>
              <div>
                <h4 className="font-bold text-slate-800 text-lg">Technician</h4>
                <p className="text-xs text-slate-600">지원형 - 설치 오브젝트</p>
              </div>
            </div>
            <div className="space-y-2 text-sm">
              <div className="bg-white p-3 rounded border border-green-200">
                <div className="flex justify-between items-center mb-1">
                  <span className="font-bold text-green-700">E: 지뢰 (Mine)</span>
                  <span className="text-xs bg-green-100 px-2 py-1 rounded">20초 쿨다운</span>
                </div>
                <p className="text-slate-600 text-xs">밟으면 폭발 (50 데미지). 30초 지속, 반경 100px</p>
                <pre className="mt-2 bg-slate-800 p-2 rounded text-[10px] text-green-400 overflow-x-auto">sendSkillUse(1, "TACTICAL", x, y);</pre>
              </div>
              <div className="bg-white p-3 rounded border border-green-200">
                <div className="flex justify-between items-center mb-1">
                  <span className="font-bold text-green-700">R: 터렛 (Turret)</span>
                  <span className="text-xs bg-green-100 px-2 py-1 rounded">40초 쿨다운</span>
                </div>
                <p className="text-slate-600 text-xs">자동 사격 포탑 (25 데미지). 20초 지속, 반경 150px</p>
                <pre className="mt-2 bg-slate-800 p-2 rounded text-[10px] text-green-400 overflow-x-auto">placeObject("turret", x, y, 20f);</pre>
              </div>
            </div>
          </div>

          {/* General */}
          <div className="bg-gradient-to-br from-yellow-50 to-amber-50 p-5 rounded-lg border-2 border-yellow-200">
            <div className="flex items-center gap-3 mb-3">
              <div className="h-12 w-12 bg-yellow-600 rounded-full flex items-center justify-center text-white font-bold text-xl">G</div>
              <div>
                <h4 className="font-bold text-slate-800 text-lg">General</h4>
                <p className="text-xs text-slate-600">밸런스형 - 지휘관</p>
              </div>
            </div>
            <div className="space-y-2 text-sm">
              <div className="bg-white p-3 rounded border border-yellow-200">
                <div className="flex justify-between items-center mb-1">
                  <span className="font-bold text-yellow-700">E: 지휘 오라 (Aura)</span>
                  <span className="text-xs bg-yellow-100 px-2 py-1 rounded">15초 쿨다운</span>
                </div>
                <p className="text-slate-600 text-xs">반경 150px 아군 이속/공속 증가. 10초 지속</p>
                <pre className="mt-2 bg-slate-800 p-2 rounded text-[10px] text-green-400 overflow-x-auto">moveSpeedMultiplier = 1.2f;</pre>
              </div>
              <div className="bg-white p-3 rounded border border-yellow-200">
                <div className="flex justify-between items-center mb-1">
                  <span className="font-bold text-yellow-700">R: 공습 (Airstrike)</span>
                  <span className="text-xs bg-yellow-100 px-2 py-1 rounded">45초 쿨다운</span>
                </div>
                <p className="text-slate-600 text-xs">미니맵 지정 후 2초 뒤 광역 폭발 (60 데미지)</p>
                <pre className="mt-2 bg-slate-800 p-2 rounded text-[10px] text-green-400 overflow-x-auto">awaitingMinimapTarget = true;</pre>
              </div>
            </div>
          </div>
        </div>

        <div className="mt-6 p-4 bg-purple-50 rounded-lg border-l-4 border-purple-500">
          <p className="text-sm text-slate-700">
            <strong>📌 참고:</strong> 나머지 6개 캐릭터 (Ghost, Skull, Sage, Steam, Wildcat, Bulldog)는 
            구현되었으나 밸런스 조정을 위해 현재 비활성화 상태입니다.
          </p>
        </div>
      </div>

      {/* 전투 시스템 */}
      <div className="bg-white rounded-xl shadow-sm p-6 border border-slate-100">
        <h3 className="text-xl font-bold text-slate-800 mb-4">전투 시스템</h3>
        
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          <div>
            <h4 className="font-bold text-slate-700 mb-3">충돌 처리 (Collision Detection)</h4>
            <div className="space-y-2 text-sm text-slate-600">
              <div className="flex items-start">
                <span className="bg-emerald-100 text-emerald-700 px-2 py-1 rounded text-xs font-bold mr-2">히트박스</span>
                <span>원형 충돌 감지 (Circle Collision)</span>
              </div>
              <div className="flex items-start">
                <span className="bg-emerald-100 text-emerald-700 px-2 py-1 rounded text-xs font-bold mr-2">쿨다운</span>
                <span>200ms 피격 무적 시간 (연속 데미지 방지)</span>
              </div>
              <div className="flex items-start">
                <span className="bg-emerald-100 text-emerald-700 px-2 py-1 rounded text-xs font-bold mr-2">벽 충돌</span>
                <span>타일 기반 장애물 검사</span>
              </div>
            </div>
          </div>

          <div>
            <h4 className="font-bold text-slate-700 mb-3">데미지 시스템</h4>
            <div className="space-y-2 text-sm text-slate-600">
              <div className="flex items-start">
                <span className="bg-amber-100 text-amber-700 px-2 py-1 rounded text-xs font-bold mr-2">Raven</span>
                <span>15 데미지, 0.3초 쿨다운 (DPS: 50)</span>
              </div>
              <div className="flex items-start">
                <span className="bg-amber-100 text-amber-700 px-2 py-1 rounded text-xs font-bold mr-2">Piper</span>
                <span>80 데미지, 1.2초 쿨다운 (DPS: 66.4)</span>
              </div>
              <div className="flex items-start">
                <span className="bg-amber-100 text-amber-700 px-2 py-1 rounded text-xs font-bold mr-2">Tech</span>
                <span>20 데미지, 0.4초 쿨다운 (DPS: 50)</span>
              </div>
              <div className="flex items-start">
                <span className="bg-amber-100 text-amber-700 px-2 py-1 rounded text-xs font-bold mr-2">General</span>
                <span>25 데미지, 0.4초 쿨다운 (DPS: 62.5)</span>
              </div>
              <div className="flex items-start">
                <span className="bg-amber-100 text-amber-700 px-2 py-1 rounded text-xs font-bold mr-2">팀킬 방지</span>
                <span>같은 팀 데미지 무효화</span>
              </div>
            </div>
          </div>
        </div>

        <div className="mt-4 p-3 bg-slate-800 rounded text-xs text-green-400 font-mono">
          <div>// 충돌 감지 예시 코드</div>
          <div className="text-slate-400">double dx = missile.x - player.x;</div>
          <div className="text-slate-400">double dy = missile.y - player.y;</div>
          <div className="text-slate-400">double distance = Math.sqrt(dx*dx + dy*dy);</div>
          <div className="text-yellow-300">if (distance {'<'} hitboxRadius) {'{'} applyDamage(); {'}'}</div>
        </div>
      </div>

      {/* 라운드 시스템 */}
      <div className="bg-white rounded-xl shadow-sm p-6 border border-slate-100">
        <h3 className="text-xl font-bold text-slate-800 mb-4">라운드 기반 게임플레이</h3>
        
        <div className="grid grid-cols-1 md:grid-cols-4 gap-4 mb-4">
          <div className="text-center p-4 bg-slate-50 rounded-lg border border-slate-200">
            <div className="text-3xl font-bold text-slate-800">3판 2선승</div>
            <div className="text-sm text-slate-500 mt-1">라운드 승리 목표</div>
          </div>
          <div className="text-center p-4 bg-slate-50 rounded-lg border border-slate-200">
            <div className="text-3xl font-bold text-slate-800">5:00</div>
            <div className="text-sm text-slate-500 mt-1">라운드 제한 시간</div>
          </div>
          <div className="text-center p-4 bg-slate-50 rounded-lg border border-slate-200">
            <div className="text-3xl font-bold text-slate-800">10초</div>
            <div className="text-sm text-slate-500 mt-1">준비/전환 시간</div>
          </div>
          <div className="text-center p-4 bg-slate-50 rounded-lg border border-slate-200">
            <div className="text-3xl font-bold text-slate-800">5초</div>
            <div className="text-sm text-slate-500 mt-1">리스폰 타이머</div>
          </div>
        </div>

        <div className="space-y-3">
          <div className="flex items-start">
            <span className="flex-shrink-0 h-6 w-6 flex items-center justify-center rounded-full bg-blue-100 text-blue-600 font-bold text-xs mr-3">1</span>
            <div>
              <div className="font-bold text-slate-700">전멸 승리 (Elimination)</div>
              <div className="text-sm text-slate-600">상대 팀 전원을 사살하면 라운드 승리</div>
            </div>
          </div>
          <div className="flex items-start">
            <span className="flex-shrink-0 h-6 w-6 flex items-center justify-center rounded-full bg-blue-100 text-blue-600 font-bold text-xs mr-3">2</span>
            <div>
              <div className="font-bold text-slate-700">시간 초과 (Timeout)</div>
              <div className="text-sm text-slate-600">5분 경과 시 생존자가 많은 팀 승리</div>
            </div>
          </div>
          <div className="flex items-start">
            <span className="flex-shrink-0 h-6 w-6 flex items-center justify-center rounded-full bg-blue-100 text-blue-600 font-bold text-xs mr-3">3</span>
            <div>
              <div className="font-bold text-slate-700">리스폰 (Respawn)</div>
              <div className="text-sm text-slate-600">라운드 시작 시 모든 플레이어 부활 및 초기화</div>
            </div>
          </div>
        </div>
      </div>

      {/* 성능 최적화 */}
      <div className="bg-white rounded-xl shadow-sm p-6 border border-slate-100">
        <h3 className="text-xl font-bold text-slate-800 mb-4">성능 최적화</h3>
        
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          <div>
            <h4 className="font-bold text-slate-700 mb-3 flex items-center gap-2">
              <span className="text-2xl">⚡</span>
              60 FPS 게임 루프
            </h4>
            <ul className="space-y-2 text-sm text-slate-600">
              <li className="flex items-start">
                <span className="text-blue-600 mr-2">•</span>
                <span>16ms마다 로직 업데이트 + 렌더링</span>
              </li>
              <li className="flex items-start">
                <span className="text-blue-600 mr-2">•</span>
                <span>고정 프레임 레이트 (Fixed Time Step)</span>
              </li>
              <li className="flex items-start">
                <span className="text-blue-600 mr-2">•</span>
                <span>더블 버퍼링으로 화면 깜빡임 방지</span>
              </li>
            </ul>
          </div>

          <div>
            <h4 className="font-bold text-slate-700 mb-3 flex items-center gap-2">
              <span className="text-2xl">🚀</span>
              네트워크 최적화
            </h4>
            <ul className="space-y-2 text-sm text-slate-600">
              <li className="flex items-start">
                <span className="text-blue-600 mr-2">•</span>
                <span>위치 업데이트: 이동 시에만 전송</span>
              </li>
              <li className="flex items-start">
                <span className="text-blue-600 mr-2">•</span>
                <span>멀티스레드 서버: 클라이언트당 독립 스레드</span>
              </li>
              <li className="flex items-start">
                <span className="text-blue-600 mr-2">•</span>
                <span>압축 프로토콜: 텍스트 기반 경량 메시지</span>
              </li>
            </ul>
          </div>
        </div>
      </div>
    </div>
  );
};

export default FeaturesSection;

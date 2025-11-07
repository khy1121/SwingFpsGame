# 다중 클라이언트 테스트 가이드

## 테스트 목적
캐릭터 선택 시스템의 동작을 확인하고 다중 플레이어 환경에서 검증합니다.

## 테스트 준비

### 1. 서버 실행
```powershell
cd C:\Users\rlagj\eclipse-workspace\NetFps
java -cp bin com.fpsgame.server.GameServer
```

### 2. 클라이언트 실행 (3개 창)
각각 별도의 터미널에서 실행:
```powershell
# 클라이언트 1
java -cp bin com.fpsgame.client.MainLauncher

# 클라이언트 2  
java -cp bin com.fpsgame.client.MainLauncher

# 클라이언트 3
java -cp bin com.fpsgame.client.MainLauncher
```

## 테스트 시나리오

### 시나리오 1: 기본 캐릭터 선택 흐름
1. 클라이언트 1: 닉네임 "Player1" 입력 → 로비 접속
2. 클라이언트 1: RED TEAM 선택
3. 클라이언트 1: "캐릭터 선택" 버튼 클릭
4. 클라이언트 1: Raven 캐릭터 선택 → 확인
5. 클라이언트 1: READY 버튼 클릭
6. **예상 결과**: 
   - 채팅창에 "Player1 님이 Raven 캐릭터를 선택했습니다!" 표시
   - RED 팀 목록에 Player1 표시
   - Player1 슬롯 배경 초록색으로 변경

### 시나리오 2: 다중 플레이어 팀 선택
1. 클라이언트 2: 닉네임 "Player2" 입력 → 로비 접속
2. 클라이언트 2: RED TEAM 선택
3. 클라이언트 2: "캐릭터 선택" 버튼 클릭 → Piper 선택
4. 클라이언트 2: READY 클릭
5. 클라이언트 3: 닉네임 "Player3" 입력 → 로비 접속
6. 클라이언트 3: BLUE TEAM 선택
7. 클라이언트 3: "캐릭터 선택" 버튼 클릭 → Bulldog 선택
8. 클라이언트 3: READY 클릭
9. **예상 결과**:
   - 모든 클라이언트에서 팀 구성 동기화 확인
   - RED 팀: Player1 (Raven), Player2 (Piper)
   - BLUE 팀: Player3 (Bulldog)
   - 모두 READY 상태이므로 START 버튼 활성화

### 시나리오 3: 캐릭터 중복 선택 제한 (향후 구현)
1. 클라이언트 1: RED TEAM, Raven 선택, READY
2. 클라이언트 2: RED TEAM, "캐릭터 선택" 클릭
3. **예상 결과** (향후):
   - Raven 캐릭터 카드가 회색으로 비활성화 표시
   - "(선택됨)" 텍스트 표시
   - 클릭 불가

### 시나리오 4: 게임 시작
1. 모든 플레이어 READY 상태
2. 팀 밸런스 조건 충족 (각 팀 1명 이상, 차이 2명 이하)
3. 아무 플레이어나 START 버튼 클릭
4. **예상 결과**:
   - 서버에서 GAME_START 신호 전송
   - 모든 클라이언트에서 GamePanel 실행
   - 선택한 캐릭터 정보가 게임에 반영

## 테스트 체크리스트

- [ ] 서버 정상 실행
- [ ] 3개 클라이언트 접속 성공
- [ ] 팀 선택 동기화 확인
- [ ] 캐릭터 선택 다이얼로그 표시
- [ ] 캐릭터 선택 정보 서버 전송
- [ ] 캐릭터 선택 채팅 알림
- [ ] READY 상태 동기화
- [ ] START 버튼 활성화 조건 확인
- [ ] 게임 시작 (GamePanel 실행)

## 알려진 이슈
1. **캐릭터 중복 선택 제한**: 현재 서버에서 추적만 하고 클라이언트에 전달하지 않음
2. **캐릭터 이미지**: assets/characters/ 폴더에 이미지 파일 필요

## 다음 단계
- [ ] 서버에서 팀별 선택된 캐릭터 목록 관리
- [ ] TEAM_ROSTER 메시지에 캐릭터 정보 포함
- [ ] 클라이언트에서 선택된 캐릭터 목록 받아 다이얼로그에 전달
- [ ] 캐릭터 이미지 추가

package com.fpsgame.client;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/**
 * 스프라이트 애니메이션 클래스
 * 
 * 캐릭터의 워킹 애니메이션, 스킬 이펙트 등을 관리합니다.
 * 프레임 기반 애니메이션으로 일정 시간마다 프레임을 전환합니다.
 */
public class SpriteAnimation {
    /** 애니메이션 프레임 배열 */
    private final BufferedImage[] frames;
    
    /** 현재 표시 중인 프레임 인덱스 */
    private int currentFrame;
    
    /** 마지막 프레임 갱신 시간 */
    private long lastTime;
    
    /** 각 프레임의 표시 시간 (밀리초) */
    private final long frameDuration;
    
    /** 애니메이션 반복 여부 */
    private final boolean loop;
    
    /** 애니메이션 종료 여부 (loop=false일 때만 의미) */
    private boolean isFinished;

    /**
     * 스프라이트 애니메이션 생성자
     * 
     * @param frames 애니메이션 프레임 이미지 배열
     * @param frameDuration 각 프레임의 표시 시간 (밀리초)
     * @param loop 애니메이션 반복 여부
     */
    public SpriteAnimation(BufferedImage[] frames, long frameDuration, boolean loop) {
        this.frames = frames;
        this.frameDuration = frameDuration;
        this.loop = loop;
        this.currentFrame = 0;
        this.lastTime = System.currentTimeMillis();
        this.isFinished = false;
    }

    /**
     * 애니메이션 상태 업데이트
     * 
     * 프레임 전환 시간을 체크하고 다음 프레임으로 이동합니다.
     * 게임 루프마다 호출되어야 합니다.
     */
    public void update() {
        // 이미 종료된 애니메이션은 업데이트하지 않음
        if (isFinished)
            return;

        long now = System.currentTimeMillis();
        
        // 프레임 지속 시간이 지났는지 확인
        if (now - lastTime >= frameDuration) {
            currentFrame++;
            lastTime = now;
            
            // 마지막 프레임에 도달했을 때 처리
            if (currentFrame >= frames.length) {
                if (loop) {
                    // 반복 애니메이션: 첫 프레임으로 돌아감
                    currentFrame = 0;
                } else {
                    // 일회성 애니메이션: 마지막 프레임에서 정지
                    currentFrame = frames.length - 1;
                    isFinished = true;
                }
            }
        }
    }

    /**
     * 현재 프레임을 화면에 그리기
     * 
     * @param g Graphics2D 컨텍스트
     * @param x 그릴 위치 X 좌표
     * @param y 그릴 위치 Y 좌표
     * @param width 그릴 너비
     * @param height 그릴 높이
     */
    public void draw(Graphics2D g, int x, int y, int width, int height) {
        if (frames != null && frames.length > 0) {
            g.drawImage(frames[currentFrame], x, y, width, height, null);
        }
    }

    /**
     * 애니메이션을 처음부터 다시 시작
     * 
     * 현재 프레임을 0으로 초기화하고 종료 상태를 해제합니다.
     */
    public void reset() {
        currentFrame = 0;
        lastTime = System.currentTimeMillis();
        isFinished = false;
    }

    /**
     * 애니메이션 종료 여부 확인
     * 
     * @return 애니메이션이 종료되었으면 true (loop=false일 때만 의미)
     */
    public boolean isFinished() {
        return isFinished;
    }
}

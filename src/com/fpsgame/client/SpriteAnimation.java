package com.fpsgame.client;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

public class SpriteAnimation {
    private BufferedImage[] frames;
    private int currentFrame;
    private long lastTime;
    private long frameDuration;
    private boolean loop;
    private boolean isFinished;

    public SpriteAnimation(BufferedImage[] frames, long frameDuration, boolean loop) {
        this.frames = frames;
        this.frameDuration = frameDuration;
        this.loop = loop;
        this.currentFrame = 0;
        this.lastTime = System.currentTimeMillis();
        this.isFinished = false;
    }

    public void update() {
        if (isFinished)
            return;

        long now = System.currentTimeMillis();
        if (now - lastTime >= frameDuration) {
            currentFrame++;
            lastTime = now;
            if (currentFrame >= frames.length) {
                if (loop) {
                    currentFrame = 0;
                } else {
                    currentFrame = frames.length - 1;
                    isFinished = true;
                }
            }
        }
    }

    public void draw(Graphics2D g, int x, int y, int width, int height) {
        if (frames != null && frames.length > 0) {
            g.drawImage(frames[currentFrame], x, y, width, height, null);
        }
    }

    public void reset() {
        currentFrame = 0;
        lastTime = System.currentTimeMillis();
        isFinished = false;
    }

    public boolean isFinished() {
        return isFinished;
    }
}

package com.fpsgame.client.effects;

import java.awt.Color;
import java.awt.Graphics2D;

public class TurretShootEffect extends SkillEffect {
    private final double angle;

    public TurretShootEffect(double angle) {
        super("turret_shoot", 0.18f); // 0.18초(약 200ms)
        this.angle = angle;
    }

    @Override
    public void drawSelf(Graphics2D g, int x, int y) {
        // Draw a brief muzzle flash in turret's direction
        int len = 18;
        int fx = (int) (x + Math.cos(angle) * len);
        int fy = (int) (y + Math.sin(angle) * len);
        float alpha = Math.max(0f, Math.min(1f, remaining / duration));
        g.setColor(new Color(255, 220, 80, (int)(180 * alpha)));
        g.drawLine(x, y, fx, fy);
        g.fillOval(fx-4, fy-4, 8, 8);
    }
}

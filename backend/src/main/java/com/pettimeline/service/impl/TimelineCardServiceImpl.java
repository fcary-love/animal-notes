package com.pettimeline.service.impl;

import com.pettimeline.exception.BusinessException;
import com.pettimeline.mapper.MomentMapper;
import com.pettimeline.mapper.PetMapper;
import com.pettimeline.model.entity.Moment;
import com.pettimeline.model.entity.Pet;
import com.pettimeline.service.FileService;
import com.pettimeline.service.TimelineCardService;
import lombok.RequiredArgsConstructor;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TimelineCardServiceImpl implements TimelineCardService {

    private final PetMapper petMapper;
    private final MomentMapper momentMapper;
    private final FileService fileService;

    @Value("${app.upload.path:./uploads}")
    private String uploadPath;

    private static final int W = 800;
    private static final int H = 600;
    private static final Color BG = new Color(0xF5, 0xF0, 0xE2);
    private static final Color ACCENT = new Color(0x5B, 0x7B, 0x3E);
    private static final Color TEXT = new Color(0x3D, 0x32, 0x2B);
    private static final Color MUTED = new Color(0x8B, 0x80, 0x73);
    private static final Color LINE = new Color(0xD4, 0xCC, 0xBE);
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy.MM.dd");

    @Override
    public String generateCard(Long userId, Long petId) {
        Pet pet = petMapper.findByIdAndUserId(petId, userId);
        if (pet == null) throw new BusinessException(404, "宠物不存在");

        List<Moment> milestones = momentMapper.findMilestonesByPetId(petId);

        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            // background
            g.setColor(BG);
            g.fillRect(0, 0, W, H);

            // decorative border
            g.setColor(LINE);
            g.setStroke(new BasicStroke(2f));
            g.draw(new RoundRectangle2D.Float(12, 12, W - 24, H - 24, 8, 8));
            g.setStroke(new BasicStroke(1f));
            g.draw(new RoundRectangle2D.Float(18, 18, W - 36, H - 36, 6, 6));

            // avatar area (centered, top third)
            int avatarSize = 140;
            int avatarX = (W - avatarSize) / 2;
            int avatarY = 60;
            BufferedImage avatar = loadAvatar(pet.getAvatarUrl());
            if (avatar != null) {
                BufferedImage circle = makeCircular(avatar, avatarSize);
                g.drawImage(circle, avatarX, avatarY, null);
            } else {
                // placeholder circle
                g.setColor(LINE);
                g.fill(new Ellipse2D.Float(avatarX, avatarY, avatarSize, avatarSize));
            }

            // pet name
            Font nameFont = loadFont("Microsoft YaHei", Font.BOLD, 36);
            g.setFont(nameFont);
            g.setColor(TEXT);
            String name = pet.getName();
            if (pet.getBreed() != null && !pet.getBreed().isEmpty()) {
                name += " · " + pet.getBreed();
            }
            FontRenderContext frc = g.getFontRenderContext();
            int nameW = (int) nameFont.getStringBounds(name, frc).getWidth();
            g.drawString(name, (W - nameW) / 2, avatarY + avatarSize + 40);

            // species subtitle
            Font subFont = loadFont("Microsoft YaHei", Font.PLAIN, 16);
            g.setFont(subFont);
            g.setColor(MUTED);
            String species = pet.getSpecies();
            if (pet.getBirthday() != null) {
                species += "  ·  " + pet.getBirthday().format(FMT) + " 出生";
            }
            int subW = (int) subFont.getStringBounds(species, frc).getWidth();
            g.drawString(species, (W - subW) / 2, avatarY + avatarSize + 66);

            // divider
            int dividerY = avatarY + avatarSize + 90;
            g.setColor(LINE);
            g.drawLine(100, dividerY, W - 100, dividerY);

            // milestones
            int msY = dividerY + 36;
            if (milestones.isEmpty()) {
                g.setFont(subFont);
                g.setColor(MUTED);
                String msg = "还没有里程碑时刻，快去记录吧";
                int mw = (int) subFont.getStringBounds(msg, frc).getWidth();
                g.drawString(msg, (W - mw) / 2, msY + 30);
            } else {
                Font msFont = loadFont("Microsoft YaHei", Font.PLAIN, 15);
                Font msDateFont = loadFont("JetBrains Mono", Font.PLAIN, 14);
                int maxItems = Math.min(milestones.size(), 5);
                for (int i = 0; i < maxItems; i++) {
                    Moment m = milestones.get(i);
                    int rowY = msY + i * 44;
                    // dot
                    g.setColor(ACCENT);
                    g.fill(new Ellipse2D.Float(140, rowY + 4, 8, 8));
                    // date
                    g.setFont(msDateFont);
                    g.setColor(MUTED);
                    g.drawString(m.getOccurredAt().format(FMT), 162, rowY + 14);
                    // label
                    g.setFont(msFont);
                    g.setColor(TEXT);
                    String label = m.getMilestoneLabel() != null ? m.getMilestoneLabel() : "";
                    g.drawString(label, 280, rowY + 14);
                    // content preview
                    if (m.getContent() != null && !m.getContent().isEmpty()) {
                        String preview = m.getContent().length() > 28 ? m.getContent().substring(0, 28) + "..." : m.getContent();
                        g.setFont(subFont);
                        g.setColor(MUTED);
                        float px = 280 + (float) msFont.getStringBounds(label, frc).getWidth() + 20;
                        g.drawString(preview, px, rowY + 14);
                    }
                }
                if (milestones.size() > 5) {
                    g.setFont(subFont);
                    g.setColor(MUTED);
                    g.drawString("...还有 " + (milestones.size() - 5) + " 个里程碑", 162, msY + maxItems * 44 + 14);
                }
            }

            // footer
            Font footerFont = loadFont("Microsoft YaHei", Font.PLAIN, 11);
            g.setFont(footerFont);
            g.setColor(MUTED);
            g.drawString("宠物时间简史 · PetTimeline", 24, H - 24);

            g.dispose();

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ImageIO.write(img, "png", bos);
            return fileService.uploadFile(bos.toByteArray(), "card.png", "cards");

        } catch (IOException e) {
            g.dispose();
            throw new BusinessException(500, "卡片生成失败");
        }
    }

    private BufferedImage loadAvatar(String avatarUrl) throws IOException {
        if (avatarUrl == null || avatarUrl.isEmpty()) return null;
        File file = Paths.get(uploadPath, avatarUrl.replace("/files/", "")).toFile();
        if (!file.exists()) return null;
        return ImageIO.read(file);
    }

    private BufferedImage makeCircular(BufferedImage source, int size) throws IOException {
        BufferedImage scaled = Thumbnails.of(source).size(size, size).asBufferedImage();
        BufferedImage circle = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = circle.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setClip(new Ellipse2D.Float(0, 0, size, size));
        g.drawImage(scaled, 0, 0, null);
        g.dispose();
        return circle;
    }

    private Font loadFont(String name, int style, int size) {
        Font f = new Font(name, style, size);
        if (!"Dialog".equals(f.getFamily())) return f;
        // fallback chain
        String[] fallbacks = {"SimSun", "SimHei", "Serif"};
        for (String fb : fallbacks) {
            f = new Font(fb, style, size);
            if (!"Dialog".equals(f.getFamily())) return f;
        }
        return f;
    }
}

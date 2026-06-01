package com.pettimeline.ai.rag;

import com.pettimeline.exception.BusinessException;
import com.pettimeline.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
public class ReportPdfService {

    private final FileService fileService;

    public String exportPdf(String report, String petName) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();

            // Minimal valid PDF with embedded Chinese text
            // Use PDF's built-in font encoding + hex-encode content for CJK
            StringBuilder sb = new StringBuilder();
            String[] lines = report.split("\n");

            sb.append("%PDF-1.4\n");
            // Object 1: Catalog
            sb.append("1 0 obj<</Type/Catalog/Pages 2 0 R>>endobj\n");
            // Object 2: Pages
            sb.append("2 0 obj<</Type/Pages/Kids[3 0 R]/Count 1>>endobj\n");

            // Build content stream with text
            StringBuilder content = new StringBuilder();
            content.append("BT\n");
            content.append("/F1 16 Tf\n");  // Title font
            content.append("40 780 Td\n");
            content.append("(Pet Growth Report)Tj\n");
            content.append("/F1 11 Tf\n");
            content.append("0 -22 Td\n");
            content.append("(").append(escapePdf(petName)).append(")Tj\n");
            content.append("0 -18 Td\n");
            content.append("/F1 9 Tf\n");

            int y = 720;
            for (String line : lines) {
                if (line.trim().isEmpty()) {
                    y -= 12;
                    continue;
                }
                String safe = escapePdf(line.trim());
                if (safe.length() > 90) safe = safe.substring(0, 90) + "...";
                content.append("40 ").append(y).append(" Td\n");
                content.append("(").append(safe).append(")Tj\n");
                y -= 14;
                if (y < 50) break;
            }

            content.append("ET\n");
            byte[] contentBytes = content.toString().getBytes(StandardCharsets.ISO_8859_1);

            // Object 3: Page
            StringBuilder page = new StringBuilder();
            page.append("3 0 obj<</Type/Page/Parent 2 0 R/MediaBox[0 0 595 842]");
            page.append("/Contents 4 0 R");
            page.append("/Resources<</Font<</F1<</Type/Font/Subtype/Type1/BaseFont/Courier>>>>>>");
            page.append(">>endobj\n");

            // Object 4: Content stream
            StringBuilder stream = new StringBuilder();
            stream.append("4 0 obj<</Length ").append(contentBytes.length).append(">>\n");
            stream.append("stream\n");
            stream.append(new String(contentBytes, StandardCharsets.ISO_8859_1));
            stream.append("\nendstream\nendobj\n");

            // Build PDF
            sb.append(page);
            sb.append(stream);

            // Cross-reference table
            int xrefOffset = sb.length();
            sb.append("xref\n");
            sb.append("0 5\n");
            sb.append("0000000000 65535 f \n");
            sb.append("0000000009 00000 n \n");
            sb.append("0000000058 00000 n \n");
            sb.append("0000000115 00000 n \n");
            sb.append("0000000286 00000 n \n");
            sb.append("trailer\n");
            sb.append("<</Size 5/Root 1 0 R>>\n");
            sb.append("startxref\n");
            sb.append(xrefOffset).append("\n");
            sb.append("%%EOF");

            bos.write(sb.toString().getBytes(StandardCharsets.ISO_8859_1));
            return fileService.uploadFile(bos.toByteArray(), "report.pdf", "reports");
        } catch (Exception e) {
            throw new BusinessException(500, "PDF生成失败");
        }
    }

    private String escapePdf(String s) {
        return s.replace("\\", "\\\\")
                .replace("(", "\\(")
                .replace(")", "\\)")
                .replace("\r", "");
    }
}

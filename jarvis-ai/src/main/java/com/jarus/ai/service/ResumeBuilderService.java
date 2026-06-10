package com.jarus.ai.service;

import com.jarus.ai.model.ResumeSection;
import com.jarus.ai.model.TailoredResume;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service
public class ResumeBuilderService {

    public byte[] generateDocx(TailoredResume tailored) throws IOException {
        try (XWPFDocument doc = new XWPFDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            // Title
            XWPFParagraph title = doc.createParagraph();
            title.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun titleRun = title.createRun();
            titleRun.setText("Tailored Resume");
            titleRun.setBold(true);
            titleRun.setFontSize(16);

            if (tailored.getModifiedSections() != null) {
                for (ResumeSection section : tailored.getModifiedSections()) {
                    // Section heading
                    XWPFParagraph heading = doc.createParagraph();
                    XWPFRun headingRun = heading.createRun();
                    headingRun.setText(section.getName());
                    headingRun.setBold(true);
                    headingRun.setFontSize(12);
                    headingRun.addBreak();

                    // Section content (use modified if changed, else original)
                    String content = section.isWasModified()
                            ? section.getModifiedContent()
                            : section.getOriginalContent();
                    XWPFParagraph contentPara = doc.createParagraph();
                    XWPFRun contentRun = contentPara.createRun();
                    contentRun.setText(content);
                    contentRun.setFontSize(10);
                    contentPara.setSpacingAfter(200);
                }
            }

            doc.write(out);
            return out.toByteArray();
        }
    }

    /**
     * Sanitize text for PDType1Font (latin-1 only). Replaces common Unicode
     * characters with ASCII equivalents and strips any remaining non-latin1 chars.
     */
    private String sanitizeForPdf(String text) {
        if (text == null) return "";
        StringBuilder sb = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case 0x2018: case 0x2019: case 0x201A: case 0x201B: sb.append('\''); break; // curly single quotes
                case 0x201C: case 0x201D: case 0x201E: case 0x201F: sb.append('"'); break;  // curly double quotes
                case 0x2013: case 0x2014: case 0x2012: case 0x2015: sb.append('-'); break;  // dashes
                case 0x2022: case 0x2023: case 0x2024: case 0x2043: sb.append('*'); break;  // bullets
                case 0x2026: sb.append("..."); break;                                         // ellipsis
                case 0x00A0: sb.append(' '); break;                                           // non-breaking space
                default:
                    if (c <= 0xFF) sb.append(c);
                    else sb.append('?');
            }
        }
        return sb.toString();
    }

    public byte[] generatePdf(TailoredResume tailored) throws IOException {
        try (PDDocument doc = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            float margin = 50;
            float yStart = PDRectangle.A4.getHeight() - margin;
            float yPos = yStart;
            float lineHeight = 14;

            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            PDPageContentStream stream = new PDPageContentStream(doc, page);
            PDType1Font boldFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font normalFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

            // Title
            stream.setFont(boldFont, 16);
            stream.beginText();
            stream.newLineAtOffset(margin, yPos);
            stream.showText("Tailored Resume");
            stream.endText();
            yPos -= 24;

            if (tailored.getModifiedSections() != null) {
                for (ResumeSection section : tailored.getModifiedSections()) {
                    // Check page space
                    if (yPos < margin + 50) {
                        stream.close();
                        page = new PDPage(PDRectangle.A4);
                        doc.addPage(page);
                        stream = new PDPageContentStream(doc, page);
                        yPos = yStart;
                    }

                    // Heading
                    stream.setFont(boldFont, 12);
                    stream.beginText();
                    stream.newLineAtOffset(margin, yPos);
                    stream.showText(sanitizeForPdf(section.getName()));
                    stream.endText();
                    yPos -= lineHeight + 2;

                    // Content
                    stream.setFont(normalFont, 10);
                    String content = section.isWasModified()
                            ? section.getModifiedContent()
                            : section.getOriginalContent();
                    if (content != null) {
                        for (String line : content.split("\n")) {
                            if (yPos < margin) {
                                stream.close();
                                page = new PDPage(PDRectangle.A4);
                                doc.addPage(page);
                                stream = new PDPageContentStream(doc, page);
                                yPos = yStart;
                                stream.setFont(normalFont, 10);
                            }
                            String sanitized = sanitizeForPdf(line.replace("\r", ""));
                            String truncated = sanitized.length() > 100 ? sanitized.substring(0, 97) + "..." : sanitized;
                            stream.beginText();
                            stream.newLineAtOffset(margin, yPos);
                            stream.showText(truncated);
                            stream.endText();
                            yPos -= lineHeight;
                        }
                    }
                    yPos -= 8;
                }
            }
            stream.close();
            doc.save(out);
            return out.toByteArray();
        }
    }
}

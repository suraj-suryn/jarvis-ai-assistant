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

    public byte[] generatePdf(TailoredResume tailored) throws IOException {
        try (PDDocument doc = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            float margin = 50;
            float yStart = PDRectangle.A4.getHeight() - margin;
            float yPos = yStart;
            float pageWidth = PDRectangle.A4.getWidth() - 2 * margin;
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
                    stream.showText(section.getName());
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
                            String truncated = line.length() > 100 ? line.substring(0, 97) + "..." : line;
                            stream.beginText();
                            stream.newLineAtOffset(margin, yPos);
                            stream.showText(truncated.replace("\r", ""));
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

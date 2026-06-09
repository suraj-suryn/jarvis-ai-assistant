package com.jarus.ai.service;

import com.jarus.ai.model.ParsedResume;
import com.jarus.ai.model.ResumeSection;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ResumeParserService {

    private static final String[] KNOWN_SECTIONS = {
        "SUMMARY", "OBJECTIVE", "PROFESSIONAL SUMMARY",
        "SKILLS", "TECHNICAL SKILLS", "CORE COMPETENCIES",
        "EXPERIENCE", "WORK EXPERIENCE", "PROFESSIONAL EXPERIENCE", "EMPLOYMENT",
        "EDUCATION", "ACADEMIC BACKGROUND",
        "PROJECTS", "KEY PROJECTS",
        "CERTIFICATIONS", "CERTIFICATES", "AWARDS",
        "PUBLICATIONS", "LANGUAGES", "INTERESTS", "HOBBIES",
        "CONTACT", "REFERENCES"
    };

    public ParsedResume parse(byte[] data, String fileName, String contentType) throws IOException {
        String fullText;
        String fileType;

        if (contentType != null && contentType.contains("pdf")) {
            fullText = parsePdf(data);
            fileType = "pdf";
        } else if (fileName != null && fileName.toLowerCase().endsWith(".docx")) {
            fullText = parseDocx(data);
            fileType = "docx";
        } else {
            // Fallback: try PDF, then DOCX
            try {
                fullText = parsePdf(data);
                fileType = "pdf";
            } catch (Exception e) {
                fullText = parseDocx(data);
                fileType = "docx";
            }
        }

        ParsedResume resume = new ParsedResume();
        resume.setFileName(fileName);
        resume.setFileType(fileType);
        resume.setFullText(fullText);
        resume.setUploadedAt(System.currentTimeMillis());
        resume.setSections(detectSections(fullText));
        return resume;
    }

    private String parsePdf(byte[] data) throws IOException {
        try (PDDocument doc = Loader.loadPDF(data)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(doc);
        }
    }

    private String parseDocx(byte[] data) throws IOException {
        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(data));
             XWPFWordExtractor extractor = new XWPFWordExtractor(doc)) {
            return extractor.getText();
        }
    }

    private List<ResumeSection> detectSections(String fullText) {
        List<ResumeSection> sections = new ArrayList<>();
        if (fullText == null || fullText.isEmpty()) return sections;

        // Build regex pattern from known section headers
        StringBuilder patternStr = new StringBuilder("(?im)^\\s*(");
        for (int i = 0; i < KNOWN_SECTIONS.length; i++) {
            if (i > 0) patternStr.append("|");
            patternStr.append(Pattern.quote(KNOWN_SECTIONS[i]));
        }
        patternStr.append(")\\s*[:\\-]?\\s*$");
        Pattern pattern = Pattern.compile(patternStr.toString());
        Matcher matcher = pattern.matcher(fullText);

        List<int[]> sectionBounds = new ArrayList<>();
        List<String> sectionNames = new ArrayList<>();

        while (matcher.find()) {
            sectionBounds.add(new int[]{matcher.start(), matcher.end()});
            sectionNames.add(matcher.group(1).trim().toUpperCase());
        }

        for (int i = 0; i < sectionBounds.size(); i++) {
            int contentStart = sectionBounds.get(i)[1];
            int contentEnd = (i + 1 < sectionBounds.size()) ? sectionBounds.get(i + 1)[0] : fullText.length();
            String content = fullText.substring(contentStart, contentEnd).trim();
            ResumeSection section = new ResumeSection();
            section.setName(sectionNames.get(i));
            section.setOriginalContent(content);
            section.setModifiedContent(content);
            section.setWasModified(false);
            sections.add(section);
        }

        // If no sections detected, treat entire text as one section
        if (sections.isEmpty()) {
            ResumeSection main = new ResumeSection();
            main.setName("RESUME");
            main.setOriginalContent(fullText);
            main.setModifiedContent(fullText);
            main.setWasModified(false);
            sections.add(main);
        }

        return sections;
    }
}

package self.heal.locators.service;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Enhanced Context Analyzer for intelligent page source analysis
 * Provides semantic understanding of HTML/XML structure for better RAG performance
 */
@Service
public class ContextAnalyzerService {
    
    private static final Logger LOGGER = Logger.getLogger(ContextAnalyzerService.class);
    
    // Patterns for different types of elements
    private static final Pattern FORM_ELEMENTS = Pattern.compile(
        "<(input|button|select|textarea|form)[^>]*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern INTERACTIVE_ELEMENTS = Pattern.compile(
        "<(a|button|input|select|textarea)[^>]*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern CONTAINER_ELEMENTS = Pattern.compile(
        "<(div|section|article|nav|header|footer|main|aside)[^>]*>", Pattern.CASE_INSENSITIVE);
    
    /**
     * Analyze page structure and extract semantic context
     */
    public PageAnalysis analyzePage(String pageSource) {
        if (pageSource == null || pageSource.trim().isEmpty()) {
            return new PageAnalysis();
        }
        
        PageAnalysis analysis = new PageAnalysis();
        
        try {
            // Extract different types of elements
            analysis.formElements = extractElements(pageSource, FORM_ELEMENTS);
            analysis.interactiveElements = extractElements(pageSource, INTERACTIVE_ELEMENTS);
            analysis.containerElements = extractElements(pageSource, CONTAINER_ELEMENTS);
            
            // Extract all elements with IDs
            analysis.elementsWithIds = extractElementsWithIds(pageSource);
            
            // Extract all elements with classes
            analysis.elementsWithClasses = extractElementsWithClasses(pageSource);
            
            // Extract text content
            analysis.textContent = extractTextContent(pageSource);
            
            // Detect automation framework type
            analysis.automationType = detectAutomationType(pageSource);
            
            LOGGER.debug("Page analysis completed: " + analysis.getSummary());
            
        } catch (Exception e) {
            LOGGER.error("Error analyzing page: " + e.getMessage(), e);
        }
        
        return analysis;
    }
    
    /**
     * Find elements that are semantically similar to the failed locator
     */
    public List<String> findSimilarElements(String failedLocator, String pageSource) {
        List<String> similarElements = new ArrayList<>();
        
        try {
            // Extract the target element type and attributes from failed locator
            ElementInfo targetInfo = parseLocator(failedLocator);
            
            if (targetInfo == null) {
                return similarElements;
            }
            
            // Find all elements of the same type
            Pattern elementPattern = Pattern.compile(
                "<" + targetInfo.tagName + "[^>]*>", Pattern.CASE_INSENSITIVE);
            Matcher matcher = elementPattern.matcher(pageSource);
            
            while (matcher.find() && similarElements.size() < 10) {
                String element = matcher.group();
                double similarity = calculateElementSimilarity(targetInfo, element);
                
                if (similarity > 0.3) { // Threshold for similarity
                    similarElements.add(element);
                }
            }
            
            // Sort by similarity (implementation would need similarity scores stored)
            
        } catch (Exception e) {
            LOGGER.error("Error finding similar elements: " + e.getMessage(), e);
        }
        
        return similarElements;
    }
    
    /**
     * Extract elements matching a pattern
     */
    private List<String> extractElements(String pageSource, Pattern pattern) {
        List<String> elements = new ArrayList<>();
        Matcher matcher = pattern.matcher(pageSource);
        
        while (matcher.find() && elements.size() < 50) { // Limit to avoid memory issues
            elements.add(matcher.group());
        }
        
        return elements;
    }
    
    /**
     * Extract all elements that have ID attributes
     */
    private Map<String, String> extractElementsWithIds(String pageSource) {
        Map<String, String> elementsWithIds = new HashMap<>();
        Pattern idPattern = Pattern.compile("<([a-zA-Z]+)[^>]*\\sid\\s*=\\s*['\"]([^'\"]+)['\"][^>]*>", Pattern.CASE_INSENSITIVE);
        Matcher matcher = idPattern.matcher(pageSource);
        
        while (matcher.find() && elementsWithIds.size() < 100) {
            String elementTag = matcher.group(1);
            String idValue = matcher.group(2);
            elementsWithIds.put(idValue, elementTag);
        }
        
        return elementsWithIds;
    }
    
    /**
     * Extract all elements that have class attributes
     */
    private Map<String, Set<String>> extractElementsWithClasses(String pageSource) {
        Map<String, Set<String>> elementsWithClasses = new HashMap<>();
        Pattern classPattern = Pattern.compile("<([a-zA-Z]+)[^>]*\\sclass\\s*=\\s*['\"]([^'\"]+)['\"][^>]*>", Pattern.CASE_INSENSITIVE);
        Matcher matcher = classPattern.matcher(pageSource);
        
        while (matcher.find() && elementsWithClasses.size() < 100) {
            String elementTag = matcher.group(1);
            String classValue = matcher.group(2);
            
            Set<String> classes = new HashSet<>(Arrays.asList(classValue.split("\\s+")));
            elementsWithClasses.put(elementTag, classes);
        }
        
        return elementsWithClasses;
    }
    
    /**
     * Extract text content from elements
     */
    private List<String> extractTextContent(String pageSource) {
        List<String> textContent = new ArrayList<>();
        
        // Extract text content from common elements
        Pattern textPattern = Pattern.compile(">([^<]+)<", Pattern.CASE_INSENSITIVE);
        Matcher matcher = textPattern.matcher(pageSource);
        
        while (matcher.find() && textContent.size() < 50) {
            String text = matcher.group(1).trim();
            if (text.length() > 2 && !text.matches("\\s*")) {
                textContent.add(text);
            }
        }
        
        return textContent;
    }
    
    /**
     * Detect the type of automation (Web/Mobile) based on page source
     */
    public static AutomationType detectAutomationType(String pageSource) {
        String lower = pageSource.toLowerCase();
        
        // Mobile automation indicators
        if (lower.contains("resource-id") || 
            lower.contains("android.widget") || 
            lower.contains("android.view") ||
            lower.contains("xpath") && lower.contains("@resource-id")) {
            return AutomationType.MOBILE_ANDROID;
        }
        
        if (lower.contains("ios") || lower.contains("xcuielementtype")) {
            return AutomationType.MOBILE_IOS;
        }
        
        // Web automation indicators
        if (lower.contains("<html") || lower.contains("<body") || lower.contains("<!doctype")) {
            return AutomationType.WEB;
        }
        
        return AutomationType.UNKNOWN;
    }
    
    /**
     * Parse locator to extract element information
     */
    private ElementInfo parseLocator(String locator) {
        try {
            ElementInfo info = new ElementInfo();
            
            // Extract tag name
            Pattern tagPattern = Pattern.compile("//([a-zA-Z]+)(?:\\[|$)");
            Matcher tagMatcher = tagPattern.matcher(locator);
            if (tagMatcher.find()) {
                info.tagName = tagMatcher.group(1);
            }
            
            // Extract ID
            Pattern idPattern = Pattern.compile("@id\\s*=\\s*['\"]([^'\"]+)['\"]");
            Matcher idMatcher = idPattern.matcher(locator);
            if (idMatcher.find()) {
                info.id = idMatcher.group(1);
            }
            
            // Extract classes
            Pattern classPattern = Pattern.compile("@class\\s*=\\s*['\"]([^'\"]+)['\"]");
            Matcher classMatcher = classPattern.matcher(locator);
            if (classMatcher.find()) {
                info.classes = new HashSet<>(Arrays.asList(classMatcher.group(1).split("\\s+")));
            }
            
            // Extract text
            Pattern textPattern = Pattern.compile("text\\(\\)\\s*=\\s*['\"]([^'\"]+)['\"]|@text\\s*=\\s*['\"]([^'\"]+)['\"]");
            Matcher textMatcher = textPattern.matcher(locator);
            if (textMatcher.find()) {
                info.text = textMatcher.group(1) != null ? textMatcher.group(1) : textMatcher.group(2);
            }
            
            return info;
            
        } catch (Exception e) {
            LOGGER.error("Error parsing locator: " + e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Calculate similarity between target element info and actual element
     */
    private double calculateElementSimilarity(ElementInfo targetInfo, String element) {
        double similarity = 0.0;
        
        // Tag name match
        if (targetInfo.tagName != null && element.toLowerCase().contains(targetInfo.tagName.toLowerCase())) {
            similarity += 0.3;
        }
        
        // ID match
        if (targetInfo.id != null && element.contains(targetInfo.id)) {
            similarity += 0.4;
        }
        
        // Class match
        if (targetInfo.classes != null) {
            for (String className : targetInfo.classes) {
                if (element.contains(className)) {
                    similarity += 0.2;
                }
            }
        }
        
        // Text match
        if (targetInfo.text != null && element.contains(targetInfo.text)) {
            similarity += 0.3;
        }
        
        return Math.min(similarity, 1.0);
    }
    
    // Helper classes and enums
    public enum AutomationType {
        WEB, MOBILE_ANDROID, MOBILE_IOS, UNKNOWN
    }
    
    private static class ElementInfo {
        String tagName;
        String id;
        Set<String> classes;
        String text;
    }
    
    public static class PageAnalysis {
        public List<String> formElements = new ArrayList<>();
        public List<String> interactiveElements = new ArrayList<>();
        public List<String> containerElements = new ArrayList<>();
        public Map<String, String> elementsWithIds = new HashMap<>();
        public Map<String, Set<String>> elementsWithClasses = new HashMap<>();
        public List<String> textContent = new ArrayList<>();
        public AutomationType automationType = AutomationType.UNKNOWN;
        
        public String getSummary() {
            return String.format("PageAnalysis{forms=%d, interactive=%d, containers=%d, withIds=%d, withClasses=%d, texts=%d, type=%s}",
                formElements.size(), interactiveElements.size(), containerElements.size(),
                elementsWithIds.size(), elementsWithClasses.size(), textContent.size(), automationType);
        }
    }
}
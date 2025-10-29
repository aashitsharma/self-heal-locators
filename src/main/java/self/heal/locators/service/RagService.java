package self.heal.locators.service;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import self.heal.locators.config.RagConfiguration;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * RAG (Retrieval-Augmented Generation) Service for optimizing context sent to LLMs
 * Reduces token count by extracting only relevant context from page source
 */
@Service
public class RagService {
    
    private static final Logger LOGGER = Logger.getLogger(RagService.class);
    
    private final RagConfiguration ragConfig;
    
    @Autowired
    public RagService(RagConfiguration ragConfig) {
        this.ragConfig = ragConfig;
        LOGGER.info("RagService initialized with configuration: " + 
            (ragConfig != null ? "enabled=" + ragConfig.isEnabled() : "NULL"));
    }
    
    /**
     * Simplified RAG: Divide page source into 5 chunks and return the most relevant one
     */
    public String retrieveRelevantContext(String failedLocator, String pageSource) {
        // Null safety checks
        if (ragConfig == null || failedLocator == null || pageSource == null) {
            return pageSource != null ? pageSource : "";
        }
        
        // Check if RAG is enabled
        if (!ragConfig.isEnabled()) {
            return truncatePageSource(pageSource, ragConfig.getMaxContextLength());
        }
        
        try {
            if (ragConfig.isDetailedLoggingEnabled()) {
                LOGGER.info("Starting simplified RAG for locator: " + failedLocator);
            }
            
            // Step 1: Extract locator attributes
            LocatorAttributes attributes = extractLocatorAttributes(failedLocator);
            if (ragConfig.isDetailedLoggingEnabled()) {
                LOGGER.debug("Extracted attributes: " + attributes);
            }
            
            // Step 2: Divide page source into exactly 5 chunks
            List<String> fiveChunks = divideIntoFiveChunks(pageSource);
            if (ragConfig.isDetailedLoggingEnabled()) {
                LOGGER.debug("Divided page source into " + fiveChunks.size() + " chunks");
            }
            
            // Step 3: Find the most relevant chunk
            String bestChunk = findMostRelevantChunk(fiveChunks, attributes, failedLocator);
            
            if (ragConfig.isDetailedLoggingEnabled()) {
                LOGGER.info("RAG selected best chunk. Length: " + bestChunk.length());
            }
            
            return bestChunk;
            
        } catch (Exception e) {
            LOGGER.error("Error in simplified RAG: " + e.getMessage(), e);
            // Fallback: return truncated original page source
            return truncatePageSource(pageSource, ragConfig.getMaxContextLength());
        }
    }
    
    /**
     * Extract meaningful attributes from the failed locator
     * Enhanced to handle direct resource IDs and various locator formats
     */
    private LocatorAttributes extractLocatorAttributes(String locator) {
        LocatorAttributes attributes = new LocatorAttributes();
        
        if (ragConfig.isDetailedLoggingEnabled()) {
            LOGGER.debug("Extracting attributes from locator: " + locator);
        }
        
        // Handle direct resource ID format (e.g., "com.aranoah.healthkart.plus:id/search_image")
        if (locator.matches("^[a-zA-Z0-9._]+:[a-zA-Z0-9_]+/[a-zA-Z0-9_]+$")) {
            attributes.resourceIds.add(locator);
            if (ragConfig.isDetailedLoggingEnabled()) {
                LOGGER.debug("Detected direct resource ID format: " + locator);
            }
        }
        
        // Extract IDs from XPath format
        Pattern idPattern = Pattern.compile("@id\\s*=\\s*['\"]([^'\"]+)['\"]");
        Matcher idMatcher = idPattern.matcher(locator);
        while (idMatcher.find()) {
            attributes.ids.add(idMatcher.group(1));
        }
        
        // Extract classes from XPath format
        Pattern classPattern = Pattern.compile("@class\\s*=\\s*['\"]([^'\"]+)['\"]");
        Matcher classMatcher = classPattern.matcher(locator);
        while (classMatcher.find()) {
            String[] classes = classMatcher.group(1).split("\\s+");
            attributes.classes.addAll(Arrays.asList(classes));
        }
        
        // Extract resource-id from XPath format (for mobile automation)
        Pattern resourceIdPattern = Pattern.compile("@resource-id\\s*=\\s*['\"]([^'\"]+)['\"]");
        Matcher resourceIdMatcher = resourceIdPattern.matcher(locator);
        while (resourceIdMatcher.find()) {
            attributes.resourceIds.add(resourceIdMatcher.group(1));
        }
        
        // Extract text content from XPath format
        Pattern textPattern = Pattern.compile("@text\\s*=\\s*['\"]([^'\"]+)['\"]|text\\(\\)\\s*=\\s*['\"]([^'\"]+)['\"]");
        Matcher textMatcher = textPattern.matcher(locator);
        while (textMatcher.find()) {
            String text = textMatcher.group(1) != null ? textMatcher.group(1) : textMatcher.group(2);
            if (text != null) {
                attributes.textContent.add(text);
            }
        }
        
        // Extract tag names from XPath format
        Pattern tagPattern = Pattern.compile("//([a-zA-Z]+)(?:\\[|$)");
        Matcher tagMatcher = tagPattern.matcher(locator);
        while (tagMatcher.find()) {
            attributes.tagNames.add(tagMatcher.group(1).toLowerCase());
        }
        
        // If locator contains specific Android widget classes, extract them as tag names
        if (locator.contains("android.widget.") || locator.contains("android.view.")) {
            Pattern androidClassPattern = Pattern.compile("android\\.(widget|view)\\.([A-Za-z]+)");
            Matcher androidClassMatcher = androidClassPattern.matcher(locator);
            while (androidClassMatcher.find()) {
                attributes.tagNames.add(androidClassMatcher.group(2).toLowerCase());
            }
        }
        
        // Extract other attributes (placeholder, name, etc.)
        Pattern attrPattern = Pattern.compile("@([a-zA-Z-]+)\\s*=\\s*['\"]([^'\"]+)['\"]");
        Matcher attrMatcher = attrPattern.matcher(locator);
        while (attrMatcher.find()) {
            String attrName = attrMatcher.group(1);
            String attrValue = attrMatcher.group(2);
            if (!Arrays.asList("id", "class", "resource-id", "text").contains(attrName)) {
                attributes.otherAttributes.put(attrName, attrValue);
            }
        }
        
        // Extract resource-id parts for better matching
        for (String resourceId : attributes.resourceIds) {
            // Extract the final part after '/' (e.g., "search_image" from "com.aranoah.healthkart.plus:id/search_image")
            if (resourceId.contains("/")) {
                String finalPart = resourceId.substring(resourceId.lastIndexOf("/") + 1);
                attributes.resourceIdParts.add(finalPart);
            }
            // Extract the package part before ':' (e.g., "com.aranoah.healthkart.plus")
            if (resourceId.contains(":")) {
                String packagePart = resourceId.substring(0, resourceId.indexOf(":"));
                attributes.packageNames.add(packagePart);
            }
        }
        
        if (ragConfig.isDetailedLoggingEnabled()) {
            LOGGER.debug("Extracted attributes: " + attributes);
        }
        
    
        // Extract resource-id parts for better matching
        for (String resourceId : attributes.resourceIds) {
            // Extract the final part after '/' (e.g., "search_image" from "com.aranoah.healthkart.plus:id/search_image")
            if (resourceId.contains("/")) {
                String finalPart = resourceId.substring(resourceId.lastIndexOf("/") + 1);
                attributes.resourceIdParts.add(finalPart);
            }
            // Extract the package part before ':' (e.g., "com.aranoah.healthkart.plus")
            if (resourceId.contains(":")) {
                String packagePart = resourceId.substring(0, resourceId.indexOf(":"));
                attributes.packageNames.add(packagePart);
            }
        }
        
        return attributes;
    }
    
    /**
     * Divide page source into exactly 5 equal chunks
     */
    private List<String> divideIntoFiveChunks(String pageSource) {
        List<String> chunks = new ArrayList<>();
        
        if (pageSource == null || pageSource.trim().isEmpty()) {
            // Return 5 empty chunks if no content
            for (int i = 0; i < 5; i++) {
                chunks.add("");
            }
            return chunks;
        }
        
        int totalLength = pageSource.length();
        int chunkSize = totalLength / 5;
        
        if (ragConfig.isDetailedLoggingEnabled()) {
            LOGGER.debug("Dividing " + totalLength + " chars into 5 chunks of ~" + chunkSize + " chars each");
        }
        
        for (int i = 0; i < 5; i++) {
            int start = i * chunkSize;
            int end;
            
            if (i == 4) { // Last chunk gets all remaining content
                end = totalLength;
            } else {
                end = Math.min(start + chunkSize, totalLength);
                // Try to end at a complete XML tag to maintain structure
                end = findBestBreakPoint(pageSource, end);
            }
            
            if (start < totalLength) {
                String chunk = pageSource.substring(start, end);
                chunks.add(chunk);
                
                if (ragConfig.isDetailedLoggingEnabled()) {
                    LOGGER.debug("Chunk " + (i + 1) + ": " + start + "-" + end + " (" + chunk.length() + " chars)");
                }
            } else {
                chunks.add(""); // Empty chunk if we've exceeded content
            }
        }
        
        return chunks;
    }
    
    /**
     * Find the best break point near the target position to maintain XML structure
     */
    private int findBestBreakPoint(String pageSource, int targetEnd) {
        if (targetEnd >= pageSource.length()) {
            return pageSource.length();
        }
        
        // Look for a good break point within 100 characters of the target
        int searchStart = Math.max(0, targetEnd - 50);
        int searchEnd = Math.min(pageSource.length(), targetEnd + 50);
        
        // Priority 1: End of complete XML element (after >)
        for (int i = targetEnd; i >= searchStart; i--) {
            if (pageSource.charAt(i) == '>' && i + 1 < pageSource.length() &&
                (pageSource.charAt(i + 1) == '<' || Character.isWhitespace(pageSource.charAt(i + 1)))) {
                return i + 1;
            }
        }
        
        // Priority 2: Look forward for end of element
        for (int i = targetEnd; i < searchEnd; i++) {
            if (pageSource.charAt(i) == '>' && i + 1 < pageSource.length() &&
                (pageSource.charAt(i + 1) == '<' || Character.isWhitespace(pageSource.charAt(i + 1)))) {
                return i + 1;
            }
        }
        
        // Priority 3: At least don't break in middle of a tag
        for (int i = targetEnd; i >= searchStart; i--) {
            if (pageSource.charAt(i) == '>') {
                return i + 1;
            }
        }
        
        // If no good break point found, use original target
        return targetEnd;
    }
    
    /**
     * Find the most relevant chunk from the 5 chunks
     */
    private String findMostRelevantChunk(List<String> chunks, LocatorAttributes attributes, String originalLocator) {
        double bestScore = -1.0;
        int bestChunkIndex = 0;
        String bestChunk = "";
        
        if (ragConfig.isDetailedLoggingEnabled()) {
            LOGGER.debug("Analyzing " + chunks.size() + " chunks for relevance to locator: " + originalLocator);
        }
        
        for (int i = 0; i < chunks.size(); i++) {
            String chunk = chunks.get(i);
            if (chunk == null || chunk.trim().isEmpty()) {
                continue;
            }
            
            double score = calculateSimplifiedRelevanceScore(chunk, attributes, originalLocator);
            
            if (ragConfig.isDetailedLoggingEnabled()) {
                LOGGER.debug("Chunk " + (i + 1) + " score: " + String.format("%.3f", score) + 
                           " (length: " + chunk.length() + ")");
            }
            
            if (score > bestScore) {
                bestScore = score;
                bestChunkIndex = i;
                bestChunk = chunk;
            }
        }
        
        // If no chunk scored above 0, return the middle chunk (index 2) as fallback
        if (bestScore <= 0.0 && chunks.size() >= 3) {
            bestChunk = chunks.get(2); // Middle chunk
            bestChunkIndex = 2;
            if (ragConfig.isDetailedLoggingEnabled()) {
                LOGGER.debug("No chunks scored above 0. Using middle chunk as fallback.");
            }
        }
        
        // Final fallback - return first non-empty chunk
        if (bestChunk.trim().isEmpty()) {
            for (String chunk : chunks) {
                if (chunk != null && !chunk.trim().isEmpty()) {
                    bestChunk = chunk;
                    break;
                }
            }
        }
        
        if (ragConfig.isDetailedLoggingEnabled()) {
            LOGGER.info("Selected chunk " + (bestChunkIndex + 1) + " with score " + 
                       String.format("%.3f", bestScore) + " (length: " + bestChunk.length() + ")");
        }
        
        return bestChunk;
    }
    
    /**
     * Simplified relevance scoring for 5-chunk approach
     */
    private double calculateSimplifiedRelevanceScore(String chunk, LocatorAttributes attributes, String originalLocator) {
        double score = 0.0;
        String chunkLower = chunk.toLowerCase();
        String locatorLower = originalLocator.toLowerCase();
        
        // Score 1: Direct locator match (highest priority)
        if (chunkLower.contains(locatorLower)) {
            score += 10.0;
            if (ragConfig.isDetailedLoggingEnabled()) {
                LOGGER.debug("Direct locator match found (+10.0)");
            }
        }
        
        // Score 2: Resource ID matches
        for (String resourceId : attributes.resourceIds) {
            if (chunkLower.contains(resourceId.toLowerCase())) {
                score += 8.0;
                if (ragConfig.isDetailedLoggingEnabled()) {
                    LOGGER.debug("Resource ID match: " + resourceId + " (+8.0)");
                }
            }
        }
        
        // Score 3: Resource ID parts
        for (String part : attributes.resourceIdParts) {
            if (chunkLower.contains(part.toLowerCase())) {
                score += 5.0;
                if (ragConfig.isDetailedLoggingEnabled()) {
                    LOGGER.debug("Resource ID part match: " + part + " (+5.0)");
                }
            }
        }
        
        // Score 4: Package names
        for (String packageName : attributes.packageNames) {
            if (chunkLower.contains(packageName.toLowerCase())) {
                score += 3.0;
                if (ragConfig.isDetailedLoggingEnabled()) {
                    LOGGER.debug("Package name match: " + packageName + " (+3.0)");
                }
            }
        }
        
        // Score 5: ID matches
        for (String id : attributes.ids) {
            if (chunkLower.contains(id.toLowerCase())) {
                score += 6.0;
                if (ragConfig.isDetailedLoggingEnabled()) {
                    LOGGER.debug("ID match: " + id + " (+6.0)");
                }
            }
        }
        
        // Score 6: Text content matches
        for (String text : attributes.textContent) {
            if (chunkLower.contains(text.toLowerCase())) {
                score += 4.0;
                if (ragConfig.isDetailedLoggingEnabled()) {
                    LOGGER.debug("Text match: " + text + " (+4.0)");
                }
            }
        }
        
        // Score 7: Class/tag matches
        for (String className : attributes.classes) {
            if (chunkLower.contains(className.toLowerCase())) {
                score += 2.0;
            }
        }
        
        for (String tagName : attributes.tagNames) {
            if (chunkLower.contains(tagName.toLowerCase())) {
                score += 2.0;
            }
        }
        
        // Bonus: Android/mobile specific content
        if (chunkLower.contains("android.widget.") || chunkLower.contains("resource-id=") || 
            chunkLower.contains("bounds=") || chunkLower.contains("class=")) {
            score += 1.0;
        }
        
        return score;
    }
    
    
    /**
     * Split page source into overlapping chunks (DEPRECATED - keeping for backward compatibility)
     */
    private List<String> chunkPageSource(String pageSource) {
        List<String> chunks = new ArrayList<>();
        
        if (pageSource == null || pageSource.length() <= ragConfig.getChunkSize()) {
            chunks.add(pageSource != null ? pageSource : "");
            return chunks;
        }
        
        for (int i = 0; i < pageSource.length(); i += ragConfig.getChunkSize() - ragConfig.getOverlapSize()) {
            int end = Math.min(i + ragConfig.getChunkSize(), pageSource.length());
            String chunk = pageSource.substring(i, end);
            
            // Try to break at complete tags/elements to maintain structure
            chunk = ensureValidXmlChunk(chunk, i == 0, end == pageSource.length());
            chunks.add(chunk);
            
            if (end == pageSource.length()) break;
            
            // Limit chunks for performance
            if (chunks.size() >= ragConfig.getMaxChunksToAnalyze()) {
                break;
            }
        }
        
        return chunks;
    }
    
    /**
     * Ensure chunk doesn't break in the middle of XML tags
     */
    private String ensureValidXmlChunk(String chunk, boolean isFirst, boolean isLast) {
        if (isFirst && isLast) return chunk;
        
        // Find the last complete tag in the chunk
        int lastCompleteTag = chunk.lastIndexOf(">");
        if (lastCompleteTag > chunk.length() - 100) { // If close to end, keep as is
            return chunk;
        }
        
        // Truncate to last complete tag
        if (lastCompleteTag > 0) {
            return chunk.substring(0, lastCompleteTag + 1);
        }
        
        return chunk;
    }
    
    /**
     * Score each chunk based on relevance to the failed locator
     */
    private List<ScoredChunk> scoreAndRankChunks(List<String> chunks, LocatorAttributes attributes) {
        List<ScoredChunk> scoredChunks = new ArrayList<>();
        double maxScore = 0.0;
        double totalScore = 0.0;
        
        for (int i = 0; i < chunks.size(); i++) {
            String chunk = chunks.get(i);
            double score = calculateRelevanceScore(chunk, attributes);
            totalScore += score;
            maxScore = Math.max(maxScore, score);
            
            if (score >= ragConfig.getRelevanceThreshold()) {
                scoredChunks.add(new ScoredChunk(chunk, score, i));
            }
        }
        
        // Sort by relevance score (descending)
        scoredChunks.sort((a, b) -> Double.compare(b.score, a.score));
        
        // Enhanced logging for debugging
        if (ragConfig.isDetailedLoggingEnabled()) {
            LOGGER.debug("Relevance scoring results:");
            LOGGER.debug("- Total chunks: " + chunks.size());
            LOGGER.debug("- Chunks above threshold (" + ragConfig.getRelevanceThreshold() + "): " + scoredChunks.size());
            LOGGER.debug("- Max score achieved: " + String.format("%.3f", maxScore));
            LOGGER.debug("- Average score: " + String.format("%.3f", totalScore / chunks.size()));
            
            if (scoredChunks.isEmpty()) {
                LOGGER.warn("No chunks met the relevance threshold! Consider lowering the threshold or check locator attributes.");
                LOGGER.debug("Locator attributes: " + attributes);
            }
        }
        
        return scoredChunks;
    }
    
    /**
     * Score chunks with adaptive threshold - used when initial scoring returns no results
     */
    private List<ScoredChunk> scoreAndRankChunksWithAdaptiveThreshold(List<String> chunks, LocatorAttributes attributes) {
        List<ScoredChunk> allScoredChunks = new ArrayList<>();
        
        // Score all chunks regardless of threshold
        for (int i = 0; i < chunks.size(); i++) {
            String chunk = chunks.get(i);
            double score = calculateRelevanceScore(chunk, attributes);
            allScoredChunks.add(new ScoredChunk(chunk, score, i));
        }
        
        // Sort by score (descending)
        allScoredChunks.sort((a, b) -> Double.compare(b.score, a.score));
        
        // Take top chunks even if they have low scores
        List<ScoredChunk> selectedChunks = new ArrayList<>();
        int maxChunksToTake = Math.min(5, allScoredChunks.size()); // Take top 5 chunks max
        
        for (int i = 0; i < maxChunksToTake; i++) {
            ScoredChunk chunk = allScoredChunks.get(i);
            // Only take chunks with score > 0 (some relevance)
            if (chunk.score > 0.0) {
                selectedChunks.add(chunk);
            }
        }
        
        // If still no chunks with any score, take the first few chunks anyway
        if (selectedChunks.isEmpty() && !allScoredChunks.isEmpty()) {
            selectedChunks.add(allScoredChunks.get(0)); // At least take the first chunk
            LOGGER.warn("Using first chunk with zero relevance score as last resort");
        }
        
        if (ragConfig.isDetailedLoggingEnabled()) {
            LOGGER.debug("Adaptive threshold selected " + selectedChunks.size() + " chunks from " + chunks.size() + " total");
            if (!selectedChunks.isEmpty()) {
                LOGGER.debug("Top selected chunk score: " + String.format("%.3f", selectedChunks.get(0).score));
            }
        }
        
        return selectedChunks;
    }
    
    /**
     * Calculate relevance score between chunk and locator attributes
     * Enhanced for better mobile automation support
     */
    private double calculateRelevanceScore(String chunk, LocatorAttributes attributes) {
        double score = 0.0;
        String chunkLower = chunk.toLowerCase();
        
        if (ragConfig.isDetailedLoggingEnabled() && chunk.length() < 200) {
            LOGGER.debug("Scoring chunk: " + chunk.substring(0, Math.min(100, chunk.length())) + "...");
        }
        
        // Score based on ID matches (highest priority)
        for (String id : attributes.ids) {
            if (chunkLower.contains(id.toLowerCase())) {
                score += ragConfig.getIdMatchWeight();
                if (ragConfig.isDetailedLoggingEnabled()) {
                    LOGGER.debug("ID match found: " + id + " (+" + ragConfig.getIdMatchWeight() + ")");
                }
            }
        }
        
        // Score based on full resource-id matches (highest priority for mobile)
        for (String resourceId : attributes.resourceIds) {
            if (chunkLower.contains(resourceId.toLowerCase())) {
                score += ragConfig.getResourceIdMatchWeight() * 2; // Double weight for exact resource ID match
                if (ragConfig.isDetailedLoggingEnabled()) {
                    LOGGER.debug("Full resource-id match found: " + resourceId + " (+" + (ragConfig.getResourceIdMatchWeight() * 2) + ")");
                }
            }
        }
        
        // Score based on resource-id parts matches (good priority for mobile)
        for (String resourceIdPart : attributes.resourceIdParts) {
            if (chunkLower.contains(resourceIdPart.toLowerCase())) {
                score += ragConfig.getResourceIdMatchWeight();
                if (ragConfig.isDetailedLoggingEnabled()) {
                    LOGGER.debug("Resource-id part match found: " + resourceIdPart + " (+" + ragConfig.getResourceIdMatchWeight() + ")");
                }
            }
        }
        
        // Score based on package name matches (medium priority for mobile)
        for (String packageName : attributes.packageNames) {
            if (chunkLower.contains(packageName.toLowerCase())) {
                score += ragConfig.getClassMatchWeight(); // Use class weight for package matches
                if (ragConfig.isDetailedLoggingEnabled()) {
                    LOGGER.debug("Package match found: " + packageName + " (+" + ragConfig.getClassMatchWeight() + ")");
                }
            }
        }
        
        // Score based on class matches
        for (String className : attributes.classes) {
            if (chunkLower.contains(className.toLowerCase())) {
                score += ragConfig.getClassMatchWeight();
                if (ragConfig.isDetailedLoggingEnabled()) {
                    LOGGER.debug("Class match found: " + className + " (+" + ragConfig.getClassMatchWeight() + ")");
                }
            }
        }
        
        // Score based on text content matches
        for (String text : attributes.textContent) {
            if (chunkLower.contains(text.toLowerCase())) {
                score += ragConfig.getTextMatchWeight();
                if (ragConfig.isDetailedLoggingEnabled()) {
                    LOGGER.debug("Text match found: " + text + " (+" + ragConfig.getTextMatchWeight() + ")");
                }
            }
        }
        
        // Score based on tag name matches (for both XML tags and Android class names)
        for (String tagName : attributes.tagNames) {
            if (chunkLower.contains("<" + tagName) || chunkLower.contains("android.widget." + tagName) || 
                chunkLower.contains("android.view." + tagName)) {
                score += ragConfig.getTagMatchWeight();
                if (ragConfig.isDetailedLoggingEnabled()) {
                    LOGGER.debug("Tag match found: " + tagName + " (+" + ragConfig.getTagMatchWeight() + ")");
                }
            }
        }
        
        // Score based on other attributes
        for (Map.Entry<String, String> entry : attributes.otherAttributes.entrySet()) {
            if (chunkLower.contains(entry.getValue().toLowerCase())) {
                score += ragConfig.getOtherAttributeMatchWeight();
                if (ragConfig.isDetailedLoggingEnabled()) {
                    LOGGER.debug("Other attribute match found: " + entry.getKey() + "=" + entry.getValue() + 
                               " (+" + ragConfig.getOtherAttributeMatchWeight() + ")");
                }
            }
        }
        
        // For mobile automation, boost score if chunk contains Android-specific attributes
        if (chunk.contains("android.widget.") || chunk.contains("android.view.") || 
            chunk.contains("resource-id=") || chunk.contains("bounds=") || chunk.contains("class=")) {
            score += 0.5; // Small boost for mobile-specific content
            if (ragConfig.isDetailedLoggingEnabled()) {
                LOGGER.debug("Mobile automation content boost (+0.5)");
            }
        }
        
        // Don't normalize by length for mobile XML as elements can be short but highly relevant
        
        if (ragConfig.isDetailedLoggingEnabled()) {
            LOGGER.debug("Final chunk score: " + String.format("%.3f", score));
        }
        
        return score;
    }
    
    /**
     * Build optimal context from ranked chunks within token limits
     * Includes fallback logic to ensure context is never empty
     */
    private String buildOptimalContext(List<ScoredChunk> rankedChunks, List<String> allChunks, String originalPageSource) {
        StringBuilder context = new StringBuilder();
        Set<Integer> selectedIndices = new HashSet<>();
        int currentLength = 0;
        
        // Step 1: Try to select chunks that meet relevance threshold
        for (ScoredChunk scoredChunk : rankedChunks) {
            if (currentLength + scoredChunk.content.length() <= ragConfig.getMaxContextLength()) {
                selectedIndices.add(scoredChunk.index);
                currentLength += scoredChunk.content.length();
            }
        }
        
        // Step 2: Fallback if no relevant chunks were found
        if (selectedIndices.isEmpty() && !allChunks.isEmpty()) {
            LOGGER.warn("No chunks met relevance threshold. Using fallback strategy.");
            
            // Fallback Strategy 1: Use chunks from the beginning (usually contains important elements)
            int maxChunksToTake = Math.min(3, allChunks.size()); // Take first 3 chunks as fallback
            for (int i = 0; i < maxChunksToTake; i++) {
                if (currentLength + allChunks.get(i).length() <= ragConfig.getMaxContextLength()) {
                    selectedIndices.add(i);
                    currentLength += allChunks.get(i).length();
                } else {
                    break; // Stop if we exceed max length
                }
            }
            
            // If still empty, take at least one chunk (truncated if needed)
            if (selectedIndices.isEmpty() && !allChunks.isEmpty()) {
                String firstChunk = allChunks.get(0);
                if (firstChunk.length() > ragConfig.getMaxContextLength()) {
                    firstChunk = firstChunk.substring(0, ragConfig.getMaxContextLength() - 100) + "\n<!-- ... truncated ... -->";
                }
                context.append(firstChunk);
                LOGGER.warn("Used truncated first chunk as absolute fallback");
                return context.toString();
            }
        }
        
        // Step 3: Build context from selected chunks
        if (!selectedIndices.isEmpty()) {
            // Create scored chunks for selected indices to maintain order
            List<ScoredChunk> chunksToUse = new ArrayList<>();
            for (Integer index : selectedIndices) {
                chunksToUse.add(new ScoredChunk(allChunks.get(index), 0.0, index));
            }
            
            // Sort selected chunks by their original order to maintain structure
            chunksToUse.sort((a, b) -> Integer.compare(a.index, b.index));
            
            // Build the context
            for (int i = 0; i < chunksToUse.size(); i++) {
                if (i > 0) {
                    context.append("\n<!-- ... chunk separator ... -->\n");
                }
                context.append(chunksToUse.get(i).content);
            }
        }
        
        // Step 4: Final fallback - if context is still empty, use truncated original
        if (context.length() == 0) {
            LOGGER.warn("All fallback strategies failed. Using truncated original page source.");
            return truncatePageSource(originalPageSource, ragConfig.getMaxContextLength());
        }
        
        return context.toString();
    }
    
    /**
     * Fallback method to truncate page source
     */
    private String truncatePageSource(String pageSource, int maxLength) {
        if (pageSource == null || pageSource.length() <= maxLength) {
            return pageSource != null ? pageSource : "";
        }
        
        String truncated = pageSource.substring(0, maxLength);
        int lastTagClose = truncated.lastIndexOf(">");
        
        if (lastTagClose > maxLength - 200) {
            return pageSource.substring(0, lastTagClose + 1) + "\n<!-- ... truncated ... -->";
        }
        
        return truncated + "\n<!-- ... truncated ... -->";
    }
    
    /**
     * Get context statistics for debugging/monitoring
     */
    public ContextStats getContextStats(String failedLocator, String pageSource) {
        String relevantContext = retrieveRelevantContext(failedLocator, pageSource);
        
        // Validate that context is not empty
        if (relevantContext == null || relevantContext.trim().isEmpty()) {
            LOGGER.error("RAG returned empty context! This should not happen. Using fallback.");
            relevantContext = truncatePageSource(pageSource, ragConfig.getMaxContextLength());
        }
        
        int originalLength = pageSource != null ? pageSource.length() : 0;
        int optimizedLength = relevantContext.length();
        double compressionRatio = originalLength > 0 ? ((double) optimizedLength / originalLength) * 100 : 0;
        
        return new ContextStats(originalLength, optimizedLength, compressionRatio);
    }
    
    /**
     * Validate that RAG is working correctly for a given input
     */
    public boolean validateRagOutput(String failedLocator, String pageSource) {
        try {
            String context = retrieveRelevantContext(failedLocator, pageSource);
            boolean isValid = context != null && !context.trim().isEmpty();
            
            if (!isValid) {
                LOGGER.error("RAG validation failed: empty context returned for locator: " + failedLocator);
            }
            
            return isValid;
        } catch (Exception e) {
            LOGGER.error("RAG validation failed with exception: " + e.getMessage(), e);
            return false;
        }
    }
    
    // Helper classes
    private static class LocatorAttributes {
        Set<String> ids = new HashSet<>();
        Set<String> classes = new HashSet<>();
        Set<String> resourceIds = new HashSet<>();
        Set<String> resourceIdParts = new HashSet<>(); // Parts after '/' in resource ID
        Set<String> packageNames = new HashSet<>();    // Package names from resource ID
        Set<String> textContent = new HashSet<>();
        Set<String> tagNames = new HashSet<>();
        Map<String, String> otherAttributes = new HashMap<>();
        
        @Override
        public String toString() {
            return "LocatorAttributes{" +
                "ids=" + ids +
                ", classes=" + classes +
                ", resourceIds=" + resourceIds +
                ", resourceIdParts=" + resourceIdParts +
                ", packageNames=" + packageNames +
                ", textContent=" + textContent +
                ", tagNames=" + tagNames +
                ", otherAttributes=" + otherAttributes +
                '}';
        }
    }
    
    private static class ScoredChunk {
        String content;
        double score;
        int index;
        
        ScoredChunk(String content, double score, int index) {
            this.content = content;
            this.score = score;
            this.index = index;
        }
    }
    
    public static class ContextStats {
        public final int originalLength;
        public final int optimizedLength;
        public final double compressionRatio;
        
        public ContextStats(int originalLength, int optimizedLength, double compressionRatio) {
            this.originalLength = originalLength;
            this.optimizedLength = optimizedLength;
            this.compressionRatio = compressionRatio;
        }
        
        @Override
        public String toString() {
            return String.format("ContextStats{original=%d, optimized=%d, compression=%.2f%%}", 
                originalLength, optimizedLength, compressionRatio);
        }
    }
}
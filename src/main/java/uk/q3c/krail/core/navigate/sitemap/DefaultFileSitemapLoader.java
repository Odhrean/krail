/*
 * Copyright (C) 2013 David Sowerby
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package uk.q3c.krail.core.navigate.sitemap;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.q3c.krail.core.navigate.LabelKeyForName;
import uk.q3c.krail.core.navigate.URITracker;
import uk.q3c.krail.core.view.KrailView;
import uk.q3c.krail.i18n.CurrentLocale;
import uk.q3c.krail.i18n.I18NKey;
import uk.q3c.krail.i18n.Translate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.text.Collator;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Loads the {@link MasterSitemap} with the entries contained in the files defined by subclasses of
 * {@link FileSitemapModule}
 *@deprecated see <a href="https://github.com/davidsowerby/krail/issues/375">Issue 375</a>
 * @author David Sowerby
 */
@Deprecated
public class DefaultFileSitemapLoader extends SitemapLoaderBase implements FileSitemapLoader {

    private enum SectionName {
        options, viewPackages, map, redirects
    }

    private enum ValidOption {
        appendView, labelKeys
    }

    private static Logger log = LoggerFactory.getLogger(DefaultFileSitemapLoader.class);
    private final MasterSitemap sitemap;
    private final Collator collator;
    private final Translate translate;
    private final String segmentSeparator = ";";
    // options
    private boolean appendView;
    private int blankLines;
    private int commentLines;
    private SectionName currentSection;
    private LocalDateTime endTime;
    private String labelKey;
    private Class<? extends Enum<?>> labelKeysClass;
    private LabelKeyForName lkfn;
    private Set<String> missingEnums;
    private Map<SectionName, List<String>> sections;
    private File sourceFile;
    private Map<String, SitemapFile> sources;
    private LocalDateTime startTime;

    @Inject
    public DefaultFileSitemapLoader(CurrentLocale currentLocale, Translate translate, MasterSitemap sitemap) {
        super();
        this.collator = Collator.getInstance(currentLocale.getLocale());
        this.translate = translate;
        this.sitemap = sitemap;

    }

    private void init() {
        startTime = LocalDateTime.now();
        endTime = null;
        missingEnums = new HashSet<>();

        sections = new HashMap<>();
    }

    private void processLines(String source, List<String> lines) {
        init();
        int i = 0;
        for (String line : lines) {
            divideIntoSections(source, line, i);
            i++;
        }

        // can only process if ALL required sections are present
        if (missingSections().size() == 0) {
            processOptions(source);
            processMap(source);
            checkLabelKeys();
            processRedirects();
        } else {
            addError(source, SECTION_MISSING, missingSections());
        }

        endTime = LocalDateTime.now();
    }

    /**
     * The expected syntax of a redirect is <em> fromPage  :  toPage</em>
     * <p>
     * Lines which do not contain a ':' are ignored<br>
     * Lines containing multiple ':' will only process the first two segments, the rest is ignored
     */
    private void processRedirects() {
        List<String> sectionLines = sections.get(SectionName.redirects);
        for (String redirect : sectionLines) {
            if (redirect.contains(":")) {
                Splitter splitter = Splitter.on(":")
                                            .trimResults();
                Iterable<String> split = splitter.split(redirect);
                Iterator<String> iter = split.iterator();
                String fromPage = iter.next();
                String toPage = iter.next();
                sitemap.addRedirect(fromPage, toPage);
            } else {
                addInfo(REDIRECT_INVALID, redirect);
            }
        }
    }

    private void checkLabelKeys() {
        for (MasterSitemapNode node : sitemap.getAllNodes()) {
            if (node.getLabelKey() == null) {
                labelKeyForName(null, node.getUriSegment());
            }
        }
    }

    private void parse(File file) {
        init();
        sourceFile = file;
        log.info("Loading sitemap from {}", file.getAbsolutePath());
        try {
            List<String> lines = FileUtils.readLines(file);
            processLines(file.getAbsolutePath(), lines);

        } catch (Exception e) {
            log.error("Unable to load site map", e);
        }
    }

    private void processOptions(String source) {
        List<String> sectionLines = sections.get(SectionName.options);
        String sectionName = SectionName.options.name();
        int i = 1;
        for (String line : sectionLines) {
            if (!line.contains("=")) {
                addError(source, PROPERTY_MISSING_EQUALS, i, sectionName);
            } else {
                // split the line on '='
                String[] pair = StringUtils.split(line, "=");
                String key = pair[0].trim();

                // malformed property may not have anything after the '='
                String value = (pair.length > 1) ? pair[1].trim() : null;

                // check for empty key or value
                boolean valid = true;

                if (Strings.isNullOrEmpty(key)) {
                    addError(source, PROPERTY_MISSING_KEY, i, sectionName);
                    valid = false;
                } else {
                    if (Strings.isNullOrEmpty(value)) {
                        addError(source, PROPERTY_MISSING_VALUE);
                        valid = false;
                    }
                }

                // process valid properties only
                if (valid) {
                    setOption(source, key, value);
                }
            }
            // increment line count
            i++;
        }
    }

    private void setOption(String source, String key, String value) {

        try {
            ValidOption k = ValidOption.valueOf(key);
            switch (k) {
                case appendView:
                    appendView = "true".equals(value);
                    break;

                case labelKeys:
                    labelKey = value;
                    if (!Strings.isNullOrEmpty(value)) {
                        validateLabelKeys(source);
                    }
                    break;

            }

        } catch (Exception e) {
            addWarning(source, PROPERTY_NAME_UNRECOGNISED, key);
        }

    }

    @SuppressWarnings("unchecked")
    private void validateLabelKeys(String source) {
        boolean valid = true;
        Class<?> requestedLabelKeysClass = null;
        try {

            requestedLabelKeysClass = Class.forName(labelKey);
            // enum
            if (!requestedLabelKeysClass.isEnum()) {
                valid = false;
            }

            // instance of I18NKeys
            @SuppressWarnings("rawtypes") Class<I18NKey> i18nClass = I18NKey.class;
            if (!i18nClass.isAssignableFrom(requestedLabelKeysClass)) {
                valid = false;
                addError(source, LABELKEY_DOES_NOT_IMPLEMENT_I18N_KEY, labelKey);
            }
        } catch (ClassNotFoundException e) {
            valid = false;
            addError(source, LABELKEY_NOT_IN_CLASSPATH, labelKey);
        }
        if (!valid) {
            addError(source, LABELKEY_NOT_VALID_CLASS_FOR_I18N_LABELS, labelKey);
        } else {
            labelKeysClass = (Class<? extends Enum<?>>) requestedLabelKeysClass;
            lkfn = new LabelKeyForName(labelKeysClass);
        }
    }

    public List<String> getViewPackages() {
        return sections.get(SectionName.viewPackages);
    }

    private void processMap(String source) {
        URITracker uriTracker = new URITracker();
        MapLineReader reader = new MapLineReader();
        List<String> sectionLines = sections.get(SectionName.map);
        int lineIndex = 1;
        int currentIndent = 0;
        for (String line : sectionLines) {
            MapLineRecord lineRecord = reader.processLine(this, source, lineIndex, line, currentIndent, segmentSeparator);
            if (!lineRecord.isFailed()) {
                uriTracker.track(lineRecord.getIndentLevel(), lineRecord.getSegment());

                NodeRecord nodeRecord = new NodeRecord(uriTracker.uri());
                nodeRecord.setUriSegment(lineRecord.getSegment());
                Class<? extends KrailView> viewClass = findView(source, lineRecord.getSegment(), lineRecord.getViewName());
                nodeRecord.setViewClass(viewClass);
                I18NKey labelKey = labelKeyForName(lineRecord.getKeyName(), nodeRecord.getUriSegment());
                nodeRecord.setLabelKey(labelKey);

                Splitter splitter = Splitter.on(",")
                                            .trimResults();
                Iterable<String> roles = splitter.split(lineRecord.getRoles());
                for (String role : roles) {
                    nodeRecord.addRole(role);
                }
                nodeRecord.setPageAccessControl(lineRecord.getPageAccessControl());
                currentIndent = lineRecord.getIndentLevel();
                sitemap.append(nodeRecord);
            }
            lineIndex++;

        }
    }

    public
    @Nullable
    I18NKey labelKeyForName(@Nonnull String labelKeyName, String uriSegment) {
        // gets name from segment if necessary
        String keyName = keyName(labelKeyName, uriSegment);
        // could be null if invalid label keys given
        if (lkfn != null) {
            return lkfn.keyForName(keyName, missingEnums);
        } else {
            missingEnums.add(keyName);
            return null;
        }

    }

    public String keyName(String labelKeyName, String uriSegment) {
        String keyName = labelKeyName;
        if (Strings.isNullOrEmpty(keyName)) {
            keyName = uriSegment.replace("-", " ");
            keyName = keyName.replace("_", " ");
            keyName = WordUtils.capitalize(keyName);
            // hyphen not valid in enum, but may be used in segment
            keyName = keyName.replace(" ", "_");
            return keyName;
        } else {
            return keyName;
        }
    }

    /**
     * Returns a view class for the given parameters. {@code viewName}. If {@link #appendView} is true the 'View' is appended to the
     * {@code viewName} before attempting to find its class declaration. <p/>
     * If view class is null or empty, the segment uri is used as a default name.<p/>
     * If no class can be found,  a sitemap build error is recorded
     *
     * @param source
     *         the source of the sitemap, used for error recording
     * @param segment
     *         the uri segment which relates to the page for which this view will be used
     * @param viewName
     *         a view class for the given parameters.
     */
    @SuppressWarnings("unchecked")
    private Class<? extends KrailView> findView(String source, String segment, @Nullable String viewName) {

        // if view is null use the segment
        if (Strings.isNullOrEmpty(viewName)) {
            String s = segment.replace("-", " ");
            s = WordUtils.capitalize(s);
            viewName = s.replace(" ", "_");
        }

        // user option whether to append 'View' or not
        if (appendView) {
            viewName = viewName + "View";
        }
        Class<?> viewClass = null;
        // try and find the view in the specified packages
        for (String pkg : getViewPackages()) {
            String fullViewName = pkg + "." + viewName;
            try {
                viewClass = Class.forName(fullViewName);
                if (KrailView.class.isAssignableFrom(viewClass)) {
                    return ((Class<? extends KrailView>) viewClass);
                } else {
                    addError(source, VIEW_DOES_NOT_IMPLEMENT_KRAILVIEW, fullViewName);
                }
            } catch (ClassNotFoundException e) {
                // don't need to do anything
            }

        }
        if (viewClass == null) {
            addError(source, VIEW_NOT_FOUND_IN_SPECIFIED_PACKAGES, viewName);
        }

        return null;
    }

    //
    // private int lastIndent(String line) {
    // int index = 0;
    // while (line.charAt(index) == '-') {
    // index++;
    // }
    // return index;
    // }

    /**
     * process a line of text from the file into the appropriate section
     *
     * @param line
     */
    private void divideIntoSections(String source, String line, int linenum) {
        String strippedLine = StringUtils.deleteWhitespace(line);
        if (strippedLine.startsWith("#")) {
            commentLines++;
            return;
        }
        if (Strings.isNullOrEmpty(strippedLine)) {
            blankLines++;
            return;
        }
        if (strippedLine.startsWith("[")) {
            if ((!strippedLine.endsWith("]"))) {
                addWarning(source, SECTION_MISSING_CLOSING, linenum);
            } else {
                String sectionName = strippedLine.substring(1, strippedLine.length() - 1);

                List<String> section = new ArrayList<>();
                try {
                    SectionName key = SectionName.valueOf(sectionName);
                    currentSection = key;
                    sections.put(key, section);
                } catch (IllegalArgumentException iae) {
                    addWarning(source, SECTION_NOT_VALID_FOR_SITEMAP, sectionName, getSections().toString());
                }

            }
            return;
        }

        List<String> section = sections.get(currentSection);
        if (section != null) {
            section.add(strippedLine);
        }

    }

    public int getCommentLines() {
        return commentLines;
    }

    public int getBlankLines() {
        return blankLines;
    }

    public Set<String> getSections() {
        Set<String> sections = new TreeSet<>();
        for (SectionName sectionName : SectionName.values()) {
            sections.add(sectionName.name());
        }
        return sections;
    }

    public String getLabelKey() {
        return labelKey;
    }

    public boolean isAppendView() {
        return appendView;
    }

    @SuppressWarnings("rawtypes")
    public Class<? extends Enum> getLabelKeysClass() {
        return labelKeysClass;
    }

    public Set<String> getMissingEnums() {
        return missingEnums;
    }

    public int getPagesDefined() {
        return sitemap.getNodeCount();
    }

    public int runtime() {
        int diffInNano = Duration.between(startTime, endTime)
                                 .getNano();
        return diffInNano;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public Set<String> missingSections() {
        Set<String> missing = new HashSet<>();
        for (SectionName section : SectionName.values()) {
            if (!sections.containsKey(section)) {
                missing.add(section.name());
            }
        }
        return missing;
    }

    public File getSourceFile() {
        return sourceFile;
    }

    /**
     * Sets the source of the sitemap input. See also {@link #setSources(Map)} , and {@link #getSources()}} for loading
     * order.
     *
     * @param sourceFile
     */
    public void setSourceFile(File sourceFile) {
        this.sourceFile = sourceFile;
    }

    public MasterSitemap getSitemap() {

        return sitemap;
    }

    @Override
    public boolean load() {
        if ((sources != null) && (!sources.isEmpty())) {
            for (SitemapFile source : sources.values()) {
                parse(new File(source.getFilePath()));
                StringBuilder buf = new StringBuilder();
                boolean first = true;
                if (!missingEnums.isEmpty()) {
                    for (String e : missingEnums) {
                        if (!first) {
                            buf.append(',');
                        }
                        buf.append(e);
                        first = false;
                    }
                    if (labelKeysClass != null) {
                        addError(source.getFilePath(), ENUM_MISSING, buf.toString(), labelKeysClass.getName());
                    }

                }
            }
            return true;
        } else {
            log.info("No file based sources for the Sitemap identified, nothing to load");
            return false;
        }

    }

    public ImmutableMap<String, SitemapFile> getSources() {
        return ImmutableMap.copyOf(sources);
    }

    @Inject(optional = true)
    public void setSources(Map<String, SitemapFile> sources) {
        this.sources = sources;
    }

    @Override
    public void addError(String source, String msgPattern, Object... msgParams) {
        super.addError(source, msgPattern, msgParams);
    }

    @Override
    public void addWarning(String source, String msgPattern, Object... msgParams) {
        super.addWarning(source, msgPattern, msgParams);
    }

    @Override
    public void addInfo(String source, String msgPattern, Object... msgParams) {
        super.addInfo(source, msgPattern, msgParams);
    }

    public List<String> getSourceNames() {
        List<String> sourceNames = new ArrayList<>();
        for (SitemapFile source : sources.values()) {
            sourceNames.add(source.getFilePath());
        }
        return sourceNames;
    }

}

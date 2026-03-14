package com.hhoa.kline.core.core.integrations.checkpoints;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CheckpointExclusions {

    public static final String GIT_DISABLED_SUFFIX = "_disabled";

    public static List<String> getDefaultExclusions(List<String> lfsPatterns) {
        List<String> patterns = new ArrayList<>();
        patterns.add(".git/");
        patterns.add(".git" + GIT_DISABLED_SUFFIX + "/");
        patterns.addAll(getBuildArtifactPatterns());
        patterns.addAll(getMediaFilePatterns());
        patterns.addAll(getCacheFilePatterns());
        patterns.addAll(getConfigFilePatterns());
        patterns.addAll(getLargeDataFilePatterns());
        patterns.addAll(getDatabaseFilePatterns());
        patterns.addAll(getGeospatialPatterns());
        patterns.addAll(getLogFilePatterns());
        if (lfsPatterns != null) {
            patterns.addAll(lfsPatterns);
        }
        return patterns;
    }

    private static List<String> getBuildArtifactPatterns() {
        return Arrays.asList(
                ".gradle/",
                ".idea/",
                ".parcel-cache/",
                ".pytest_cache/",
                ".next/",
                ".nuxt/",
                ".sass-cache/",
                ".vs/",
                ".vscode/",
                ".clinerules/",
                "Pods/",
                "__pycache__/",
                "bin/",
                "build/",
                "bundle/",
                "coverage/",
                "deps/",
                "dist/",
                "env/",
                "node_modules/",
                "obj/",
                "out/",
                "pycache/",
                "target/dependency/",
                "temp/",
                "vendor/",
                "venv/");
    }

    private static List<String> getMediaFilePatterns() {
        return Arrays.asList(
                "*.jpg", "*.jpeg", "*.png", "*.gif", "*.bmp", "*.ico", "*.webp", "*.tiff", "*.tif",
                "*.raw", "*.heic", "*.avif", "*.eps", "*.psd", "*.3gp", "*.aac", "*.aiff", "*.asf",
                "*.avi", "*.divx", "*.flac", "*.m4a", "*.m4v", "*.mkv", "*.mov", "*.mp3", "*.mp4",
                "*.mpeg", "*.mpg", "*.ogg", "*.opus", "*.rm", "*.rmvb", "*.vob", "*.wav", "*.webm",
                "*.wma", "*.wmv");
    }

    private static List<String> getCacheFilePatterns() {
        return Arrays.asList(
                "*.DS_Store",
                "*.bak",
                "*.cache",
                "*.crdownload",
                "*.dmp",
                "*.dump",
                "*.eslintcache",
                "*.lock",
                "*.log",
                "*.old",
                "*.part",
                "*.partial",
                "*.pyc",
                "*.pyo",
                "*.stackdump",
                "*.swo",
                "*.swp",
                "*.temp",
                "*.tmp",
                "*.Thumbs.db");
    }

    private static List<String> getConfigFilePatterns() {
        return Arrays.asList("*.env*", "*.local", "*.development", "*.production");
    }

    private static List<String> getLargeDataFilePatterns() {
        return Arrays.asList(
                "*.zip", "*.tar", "*.gz", "*.rar", "*.7z", "*.iso", "*.bin", "*.exe", "*.dll",
                "*.so", "*.dylib", "*.dat", "*.dmg", "*.msi");
    }

    private static List<String> getDatabaseFilePatterns() {
        return Arrays.asList(
                "*.arrow",
                "*.accdb",
                "*.aof",
                "*.avro",
                "*.bak",
                "*.bson",
                "*.csv",
                "*.db",
                "*.dbf",
                "*.dmp",
                "*.frm",
                "*.ibd",
                "*.mdb",
                "*.myd",
                "*.myi",
                "*.orc",
                "*.parquet",
                "*.pdb",
                "*.rdb",
                "*.sqlite");
    }

    private static List<String> getGeospatialPatterns() {
        return Arrays.asList(
                "*.shp",
                "*.shx",
                "*.dbf",
                "*.prj",
                "*.sbn",
                "*.sbx",
                "*.shp.xml",
                "*.cpg",
                "*.gdb",
                "*.mdb",
                "*.gpkg",
                "*.kml",
                "*.kmz",
                "*.gml",
                "*.geojson",
                "*.dem",
                "*.asc",
                "*.img",
                "*.ecw",
                "*.las",
                "*.laz",
                "*.mxd",
                "*.qgs",
                "*.grd",
                "*.csv",
                "*.dwg",
                "*.dxf");
    }

    private static List<String> getLogFilePatterns() {
        return Arrays.asList(
                "*.error",
                "*.log",
                "*.logs",
                "*.npm-debug.log*",
                "*.out",
                "*.stdout",
                "yarn-debug.log*",
                "yarn-error.log*");
    }

    public static void writeExcludesFile(Path gitPath, List<String> lfsPatterns)
            throws IOException {
        Path excludesPath = gitPath.resolve("info").resolve("exclude");
        Files.createDirectories(excludesPath.getParent());
        List<String> patterns = getDefaultExclusions(lfsPatterns);
        Files.write(excludesPath, patterns);
    }

    public static List<String> getLfsPatterns(String workspacePath) {
        try {
            Path attributesPath = Paths.get(workspacePath, ".gitattributes");
            if (Files.exists(attributesPath)) {
                List<String> lines = Files.readAllLines(attributesPath);
                List<String> lfsPatterns = new ArrayList<>();
                for (String line : lines) {
                    if (line.contains("filter=lfs")) {
                        String pattern = line.split("\\s+")[0].trim();
                        if (!pattern.isEmpty()) {
                            lfsPatterns.add(pattern);
                        }
                    }
                }
                return lfsPatterns;
            }
        } catch (Exception e) {
            log.warn("Failed to read .gitattributes: {}", e.getMessage());
        }
        return new ArrayList<>();
    }
}

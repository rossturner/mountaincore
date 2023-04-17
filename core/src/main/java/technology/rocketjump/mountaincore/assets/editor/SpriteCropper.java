package technology.rocketjump.mountaincore.assets.editor;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.math.Vector2;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.io.FileUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public class SpriteCropper {
    private static final int ALPHA_BAND = 3;

    //TODO: consider error logs
    public void processDirectory(Path filePath) throws Exception {
        Map<String, Path> spriteFiles = new HashMap<>();
        Path descriptorsFilePath = null;
        JSONArray descriptorsJson = null;

        try (Stream<Path> fileList = Files.list(filePath)) {
            for (Path path : fileList.toList()) {
                if (Files.isDirectory(path)) {
                    processDirectory(path);
                } else if (path.getFileName().toString().endsWith("_NORMALS.png") || path.getFileName().toString().endsWith("-swatch.png")) {
                    // Ignore normals and swatches
                } else if (path.getFileName().toString().endsWith(".png")) {
                    spriteFiles.put(path.getFileName().toString(), path);
                } else if (path.getFileName().toString().equalsIgnoreCase("descriptors.json")) {
                    descriptorsFilePath = path;
                    descriptorsJson = JSON.parseArray(FileUtils.readFileToString(path.toFile()));
                }
            }
        }

        if (descriptorsJson != null) {
            processSprites(descriptorsJson, spriteFiles);
            // write descriptors.json back in place
            Gson gson = new GsonBuilder()
                    .setPrettyPrinting()
                    .disableHtmlEscaping()
                    .create();
            String outputText = gson.toJson(descriptorsJson);

            FileUtils.write(descriptorsFilePath.toFile(), outputText);
        }
    }

    private void processSprites(JSONArray descriptorsJson, Map<String, Path> spriteFiles) throws Exception {
        Map<String, Vector2> newOffsets = new HashMap<>();

        for (Map.Entry<String, Path> entry : spriteFiles.entrySet()) {
            String filename = entry.getKey();
            System.out.println("Processing " + filename);
            Path spriteFile = entry.getValue();

            BufferedImage original = ImageIO.read(spriteFile.toFile());
            int width = original.getWidth();
            int height = original.getHeight();

            int cropLeft, cropTop, cropRight, cropBottom;
            for (cropLeft = 0; cropLeft < width; cropLeft++) {
                boolean entireLineTransparent = true;
                for (int y = 0; y < height; y++) {
                    boolean transparent = original.getData().getSample(cropLeft, y, ALPHA_BAND) == 0;
                    if (!transparent) {
                        entireLineTransparent = false;
                        break;
                    }
                }
                if (!entireLineTransparent) {
                    break;
                }
            }
            for (cropTop = 0; cropTop < height; cropTop++) {
                boolean entireLineTransparent = true;
                for (int x = 0; x < width; x++) {
                    boolean transparent = original.getData().getSample(x, height - 1 - cropTop, ALPHA_BAND) == 0;
                    if (!transparent) {
                        entireLineTransparent = false;
                        break;
                    }
                }
                if (!entireLineTransparent) {
                    break;
                }
            }
            for (cropRight = 0; cropRight < width; cropRight++) {
                boolean entireLineTransparent = true;
                for (int y = 0; y < height; y++) {
                    boolean transparent = original.getData().getSample(width - 1 - cropRight, y, ALPHA_BAND) == 0;
                    if (!transparent) {
                        entireLineTransparent = false;
                        break;
                    }
                }
                if (!entireLineTransparent) {
                    break;
                }
            }
            for (cropBottom = 0; cropBottom < height; cropBottom++) {
                boolean entireLineTransparent = true;
                for (int x = 0; x < width; x++) {
                    boolean transparent = original.getData().getSample(x, cropBottom, ALPHA_BAND) == 0;
                    if (!transparent) {
                        entireLineTransparent = false;
                        break;
                    }
                }
                if (!entireLineTransparent) {
                    break;
                }
            }
            // Reduce all by 1 for entirely transparent padding around image
            if (cropLeft > 0) {
                cropLeft--;
            }
            if (cropTop > 0) {
                cropTop--;
            }
            if (cropRight > 0) {
                cropRight--;
            }
            if (cropBottom > 0) {
                cropBottom--;
            }

            if (cropLeft > 0 || cropTop > 0 || cropRight > 0 || cropBottom > 0) {
                int newWidth = width - cropLeft - cropRight;
                int newHeight = height - cropTop - cropBottom;

                BufferedImage croppedImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);
                croppedImage.getGraphics().drawImage(original, 0, 0, newWidth, newHeight, cropLeft, cropBottom, cropLeft + newWidth, cropBottom + newHeight, null);

                ImageIO.write(croppedImage, "png", spriteFile.toFile());

                Vector2 originalMidpoint = new Vector2(((float) width) / 2f, ((float) height) / 2f);
                Vector2 newMidpoint = new Vector2(cropLeft + (((float) newWidth) / 2f), cropBottom + ((float) newHeight / 2f));
                Vector2 offset = originalMidpoint.cpy().sub(newMidpoint);
                offset.x = 0 - offset.x;
                newOffsets.put(filename, offset);
            }
        }

        processDescriptors(descriptorsJson, newOffsets);
    }

    private void processDescriptors(JSONArray descriptorsJson, Map<String, Vector2> newOffsets) {
        for (int cursor = 0; cursor < descriptorsJson.size(); cursor++) {
            JSONObject descriptorRootNode = descriptorsJson.getJSONObject(cursor);

            JSONObject spriteDescriptors = descriptorRootNode.getJSONObject("spriteDescriptors");
            if (spriteDescriptors == null) {
                System.err.println("Could not find spriteDescriptors in " + descriptorRootNode.toString());
            } else {
                for (String direction : spriteDescriptors.keySet()) {
                    JSONObject directionJson = spriteDescriptors.getJSONObject(direction);

                    boolean isFlipX = directionJson.getBooleanValue("flipX");
                    if (directionJson.get("filename") == null) {
                        continue;
                    }
                    String filename = directionJson.getString("filename");
                    System.out.println("Processing descriptors for " + filename);
                    Vector2 newOffset = newOffsets.get(filename);
                    if (newOffset != null) {
                        JSONObject offsetPixelsJson = directionJson.getJSONObject("offsetPixels");
                        if (offsetPixelsJson == null) {
                            offsetPixelsJson = new JSONObject(true);
                        }
                        if (isFlipX) {
                            newOffset = newOffset.cpy();
                            newOffset.x = 0 - newOffset.x;
                        }

                        float scale = 1f;
                        if (directionJson.getFloat("scale") != null) {
                            scale = directionJson.getFloat("scale");
                        }
                        Vector2 replacementOffset = newOffset.cpy().add(
                                offsetPixelsJson.getFloatValue("x"),
                                offsetPixelsJson.getFloatValue("y")
                        );

                        if (scale != 1f) {
                            replacementOffset.x = replacementOffset.x * scale;
                            replacementOffset.y = replacementOffset.y * scale;
                        }

                        offsetPixelsJson.put("x", replacementOffset.x);
                        offsetPixelsJson.put("y", replacementOffset.y);

                        directionJson.put("offsetPixels", offsetPixelsJson);

                        processRelatedAssets(newOffset, directionJson.getJSONArray("childAssets"), scale);
                        processRelatedAssets(newOffset, directionJson.getJSONArray("parentEntityAssets"), scale);
                        processRelatedAssets(newOffset, directionJson.getJSONArray("attachmentPoints"), scale);
                    }
                }
            }
        }
    }

    private void processRelatedAssets(Vector2 newOffset, JSONArray childAssets, float parentScale) {
        if (childAssets != null) {
            for (int childCursor = 0; childCursor < childAssets.size(); childCursor++) {
                JSONObject childAssetJson = childAssets.getJSONObject(childCursor);
                JSONObject childOffsetJson = childAssetJson.getJSONObject("offsetPixels");
                if (childOffsetJson == null) {
                    childOffsetJson = new JSONObject(true);
                }
                Vector2 childOffsetVec = new Vector2(
                        childOffsetJson.getFloatValue("x"),
                        childOffsetJson.getFloatValue("y")
                );
                childOffsetVec.sub(newOffset);
                childOffsetJson.put("x", childOffsetVec.x);
                childOffsetJson.put("y", childOffsetVec.y);
                childAssetJson.put("offsetPixels", childOffsetJson);
            }
        }
    }
}

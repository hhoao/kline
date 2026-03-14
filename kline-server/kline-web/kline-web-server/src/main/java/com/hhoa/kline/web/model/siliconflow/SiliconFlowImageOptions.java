package com.hhoa.kline.web.model.siliconflow;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.ai.image.ImageOptions;

/**
 * 硅基流动 {@link ImageOptions}
 *
 * @author hhoa
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SiliconFlowImageOptions implements ImageOptions {

    @JsonProperty("model")
    private String model;

    @JsonProperty("negative_prompt")
    private String negativePrompt;

    @JsonProperty("image_size")
    private String imageSize;

    @JsonProperty("batch_size")
    @Builder.Default
    private Integer batchSize = 1;

    @JsonProperty("num_inference_steps")
    @Builder.Default
    private Integer numInferenceSteps = 25;

    /**
     * This value is used to control the degree of match between the generated image and the given
     * prompt. The higher the value, the more the generated image will tend to strictly match the
     * text prompt. The lower the value, the more creative and diverse the generated image will be,
     * potentially containing more unexpected elements.
     *
     * <p>Required range: 0 <= x <= 20
     */
    @JsonProperty("guidance_scale")
    @Builder.Default
    private Float guidanceScale = 0.75F;

    /** 如果想要每次都生成固定的图片，可以把 seed 设置为固定值 */
    @JsonProperty("seed")
    @Builder.Default
    private Integer seed = (int) (Math.random() * 1_000_000_000);

    /** The image that needs to be uploaded should be converted into base64 format. */
    @JsonProperty("image")
    private String image;

    private Integer width;

    private Integer height;

    public void setHeight(Integer height) {
        this.height = height;
        if (this.width != null && this.height != null) {
            this.imageSize = this.width + "x" + this.height;
        }
    }

    public void setWidth(Integer width) {
        this.width = width;
        if (this.width != null && this.height != null) {
            this.imageSize = this.width + "x" + this.height;
        }
    }

    @Override
    public Integer getN() {
        return batchSize;
    }

    @Override
    public String getResponseFormat() {
        return "url";
    }

    @Override
    public String getStyle() {
        return null;
    }
}

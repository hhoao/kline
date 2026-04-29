package com.hhoa.kline.core.core.tools.args;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public record UseSkillInput(
        @JsonProperty(value = "skill_name", required = true)
                @JsonPropertyDescription("The name of the skill to activate.")
                String skillName) {}

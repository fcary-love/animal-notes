package com.pettimeline.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("pets")
public class Pet {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private String name;
    private String species;
    private String breed;
    private LocalDate birthday;
    private String avatarUrl;
    private String bio;
    private String modelConfig;
    private LocalDateTime modelUpdatedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer isDeleted;
}

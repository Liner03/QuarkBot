package com.lin.bot.model.DTO;


import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.data.redis.core.RedisHash;

import java.io.Serial;
import java.io.Serializable;

/**
 * @Author Lin.
 * @Date 2025/1/6
 */
@Data
@RedisHash("Quark")
@Accessors(chain = true)
public class QuarkDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private String name;
    private String description;
    private String url;
    private String password;
    private boolean valid;
    private boolean ending;
}

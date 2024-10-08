package ru.kardo.model;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.kardo.model.enums.DirectionEnum;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Direction {

    @Enumerated(EnumType.STRING)
    private DirectionEnum direction;
}

package com.elbuensabor.services.impl;

import com.elbuensabor.dto.response.HorarioStatusResponseDTO;
import com.elbuensabor.services.IHorarioService;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;



@Service
    public class HorarioServiceImpl implements IHorarioService {

        // ... (los atributos de horarios no cambian) ...
        private final LocalTime NOCHE_APERTURA = LocalTime.of(19, 0);
        private final LocalTime NOCHE_CIERRE = LocalTime.of(23, 59, 59);
        private final LocalTime MEDIODIA_APERTURA = LocalTime.of(11, 0);
        private final LocalTime MEDIODIA_CIERRE = LocalTime.of(15, 0);


        @Override
        public HorarioStatusResponseDTO getEstadoHorario() {
            LocalDateTime ahora = LocalDateTime.now();
            DayOfWeek dia = ahora.getDayOfWeek();
            LocalTime hora = ahora.toLocalTime();

            // Lógica para verificar si está abierto (la misma de antes)
            boolean estaAbierto = false;
            if (!hora.isBefore(NOCHE_APERTURA) && !hora.isAfter(NOCHE_CIERRE)) {
                estaAbierto = true;
            } else {
                boolean esFinDeSemana = (dia == DayOfWeek.SATURDAY || dia == DayOfWeek.SUNDAY);
                if (esFinDeSemana && !hora.isBefore(MEDIODIA_APERTURA) && !hora.isAfter(MEDIODIA_CIERRE)) {
                    estaAbierto = true;
                }
            }

            // Construir y devolver el DTO
            if (estaAbierto) {
                return new HorarioStatusResponseDTO(true, "El local se encuentra abierto.");
            } else {
                return new HorarioStatusResponseDTO(false, "El local se encuentra cerrado y no es posible realizar pedidos en este momento.");
            }
        }
    }


package com.elbuensabor.dto.response;

import com.elbuensabor.dto.request.DomicilioDTO;
import com.elbuensabor.dto.request.ImagenDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClienteResponseDTO {
    private Long idCliente;
    private String nombre;
    private String apellido;
    private String email;
    private String telefono;
    private LocalDate fechaNacimiento;
    private List<DomicilioDTO> domicilios;
    private ImagenDTO imagen;
}

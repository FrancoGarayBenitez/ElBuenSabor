package com.elbuensabor.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "cliente")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Cliente {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cliente")
    private Long idCliente;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private String apellido;

    @Column(nullable = false)
    private String telefono;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "fecha_nacimiento", nullable = false)
    private LocalDate fechaNacimiento;

    @OneToMany(mappedBy = "cliente", cascade = CascadeType.ALL)
    private List<Pedido> pedidos = new ArrayList<>();

    @ManyToMany
    @JoinTable(
            name = "cliente_domicilio",
            joinColumns = @JoinColumn(name = "id_cliente"),
            inverseJoinColumns = @JoinColumn(name = "id_domicilio")
    )
    private List<Domicilio> domicilios = new ArrayList<>();

    @OneToOne
    @JoinColumn(name = "id_usuario")
    private Usuario usuario;

    @OneToOne
    @JoinColumn(name = "id_imagen")
    private Imagen imagen;
}

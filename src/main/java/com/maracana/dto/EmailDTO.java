package com.maracana.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EmailDTO {

    @NotBlank(message = "El asunto es obligatorio")
    private String asunto;

    @NotBlank(message = "El cuerpo del mensaje es obligatorio")
    private String cuerpo;

    private List<String> destinatarios;

    private List<String> rolesDestinatarios;

    private boolean enviarATodos;

    // Métodos explícitos para evitar problemas con Lombok
    public String getAsunto() {
        return asunto;
    }

    public void setAsunto(String asunto) {
        this.asunto = asunto;
    }

    public String getCuerpo() {
        return cuerpo;
    }

    public void setCuerpo(String cuerpo) {
        this.cuerpo = cuerpo;
    }

    public List<String> getDestinatarios() {
        return destinatarios;
    }

    public void setDestinatarios(List<String> destinatarios) {
        this.destinatarios = destinatarios;
    }

    public List<String> getRolesDestinatarios() {
        return rolesDestinatarios;
    }

    public void setRolesDestinatarios(List<String> rolesDestinatarios) {
        this.rolesDestinatarios = rolesDestinatarios;
    }

    public boolean isEnviarATodos() {
        return enviarATodos;
    }

    public void setEnviarATodos(boolean enviarATodos) {
        this.enviarATodos = enviarATodos;
    }
}

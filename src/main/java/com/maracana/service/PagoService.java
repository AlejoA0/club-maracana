package com.maracana.service;

import com.maracana.dto.PagoDTO;
import com.maracana.model.Pago;
import com.maracana.model.Reserva;
import com.maracana.repository.PagoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PagoService {

    private final PagoRepository pagoRepository;
    private final ReservaService reservaService;
    
    // Valor fijo para todas las reservas: 160.000 pesos colombianos
    private static final BigDecimal MONTO_RESERVA = new BigDecimal("160000.00");
    
    /**
     * Obtiene el valor fijo de la reserva de cancha
     * @return el valor de la reserva (160.000 pesos colombianos)
     */
    public BigDecimal obtenerValorReserva() {
        return MONTO_RESERVA;
    }
    
    /**
     * Procesa el pago de una reserva
     * @param pagoDTO información del pago a procesar
     * @return mensaje de éxito o error
     */
    @Transactional
    public String procesarPago(PagoDTO pagoDTO) {
        try {
            Optional<Reserva> reservaOpt = reservaService.buscarPorId(pagoDTO.getReservaId());
            if (reservaOpt.isEmpty()) {
                return "Error: No se encontró la reserva para realizar el pago";
            }
            
            Reserva reserva = reservaOpt.get();
            
            // Verificar si ya existe un pago para esta reserva
            if (reserva.getPago() != null) {
                return "Error: Esta reserva ya tiene un pago registrado";
            }
            
            // Verificar que el monto sea correcto
            if (!pagoDTO.getMonto().equals(MONTO_RESERVA)) {
                return "Error: El monto del pago no coincide con el valor de la reserva";
            }
            
            // Crear y guardar el pago
            Pago pago = new Pago();
            pago.setReserva(reserva);
            pago.setMonto(pagoDTO.getMonto());
            pago.setMetodoPago(pagoDTO.getMetodoPago());
            pago.setFechaPago(LocalDateTime.now());
            
            pagoRepository.save(pago);
            
            // Actualizar la referencia en la reserva
            reserva.setPago(pago);
            
            log.info("Pago procesado correctamente para la reserva ID: {}", reserva.getId());
            return "Pago procesado correctamente. Su reserva ha sido confirmada.";
            
        } catch (Exception e) {
            log.error("Error al procesar el pago: {}", e.getMessage(), e);
            return "Error al procesar el pago: " + e.getMessage();
        }
    }
}

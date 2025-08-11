package boiler;

public enum Mode {
    INITIALIZATION,
    NORMAL,
    DEGRADED, // tenta manter o nível de água satisfatório mesmo com a presença de falha
    // em algum dispositivo físico.
    SALVAMENTO, // tenta manter o nível de água satisfatório mesmo com o defeito no
    // dispositivo que mede a quantidade de água na caldeira
    EMERGENCY_STOP
}
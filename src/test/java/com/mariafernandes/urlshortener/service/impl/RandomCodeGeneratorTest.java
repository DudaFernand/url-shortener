package com.mariafernandes.urlshortener.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.RepeatedTest;

import static org.junit.jupiter.api.Assertions.*;

class RandomCodeGeneratorTest {

    private final RandomCodeGenerator generator = new RandomCodeGenerator();

    @Test
    void generateCode_deveRetornarStringCom6Caracteres() {
        String code = generator.generateCode();
        assertEquals(6, code.length());
    }

    @Test
    void generateCode_deveConterApenasCaracteresAlfanumericos() {
        String code = generator.generateCode();
        assertTrue(code.matches("[a-zA-Z0-9]+"));
    }

    @RepeatedTest(50)
    void generateCode_deveGerarCodigosDiferentes() {
        String code1 = generator.generateCode();
        String code2 = generator.generateCode();
        assertNotEquals(code1, code2);
    }
}


// FileManager.java
package com.banking.util;

import com.banking.model.Account;
import com.banking.model.Transaction;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class FileManager {
    private static final String DATA_DIRECTORY = "data";
    private static final String ACCOUNTS_FILE = "accounts.json";
    private static final String TRANSACTIONS_FILE = "transactions.json";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final Gson gson;

    public FileManager() {
        this.gson = new GsonBuilder()
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
                .registerTypeAdapter(BigDecimal.class, new BigDecimalAdapter())
                .setPrettyPrinting()
                .create();

        // Crear directorio de datos si no existe
        createDataDirectory();
    }

    // Guardar datos
    public void saveAccounts(List<Account> accounts) throws IOException {
        File file = new File(DATA_DIRECTORY, ACCOUNTS_FILE);
        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(accounts, writer);
        }
    }

    public void saveTransactions(List<Transaction> transactions) throws IOException {
        File file = new File(DATA_DIRECTORY, TRANSACTIONS_FILE);
        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(transactions, writer);
        }
    }

    // Cargar datos
    public List<Account> loadAccounts() throws IOException {
        File file = new File(DATA_DIRECTORY, ACCOUNTS_FILE);
        if (!file.exists()) {
            throw new FileNotFoundException("Archivo de cuentas no encontrado: " + file.getPath());
        }

        try (FileReader reader = new FileReader(file)) {
            Type listType = new TypeToken<List<Account>>(){}.getType();
            return gson.fromJson(reader, listType);
        }
    }

    public List<Transaction> loadTransactions() throws IOException {
        File file = new File(DATA_DIRECTORY, TRANSACTIONS_FILE);
        if (!file.exists()) {
            throw new FileNotFoundException("Archivo de transacciones no encontrado: " + file.getPath());
        }

        try (FileReader reader = new FileReader(file)) {
            Type listType = new TypeToken<List<Transaction>>(){}.getType();
            return gson.fromJson(reader, listType);
        }
    }

    // Exportar a CSV
    public void exportAccountsToCSV(List<Account> accounts, String filename) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            writer.println("AccountID,AccountNumber,OwnerName,BankName,Balance,CreationDate");

            for (Account account : accounts) {
                writer.printf("%s,%s,\"%s\",\"%s\",%s,%s%n",
                        account.getAccountId(),
                        account.getAccountNumber(),
                        account.getOwnerName(),
                        account.getBankName(),
                        account.getBalance().toString(),
                        account.getCreationDate().format(DATE_FORMATTER)
                );
            }
        }
    }

    public void exportTransactionsToCSV(List<Transaction> transactions, String filename) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            writer.println("TransactionID,SourceAccountID,DestinationAccountID,Amount,Timestamp,Description,TransactionType,Status");

            for (Transaction transaction : transactions) {
                writer.printf("%s,%s,%s,%s,%s,\"%s\",%s,%s%n",
                        transaction.getTransactionId(),
                        transaction.getSourceAccount().getAccountId(),
                        transaction.getDestinationAccount().getAccountId(),
                        transaction.getAmount().toString(),
                        transaction.getTimestamp().format(DATE_FORMATTER),
                        transaction.getDescription(),
                        transaction.getTransactionType(),
                        transaction.getStatus().toString()
                );
            }
        }
    }

    // Verificar existencia de archivos
    public boolean accountsFileExists() {
        return new File(DATA_DIRECTORY, ACCOUNTS_FILE).exists();
    }

    public boolean transactionsFileExists() {
        return new File(DATA_DIRECTORY, TRANSACTIONS_FILE).exists();
    }

    // Métodos auxiliares
    private void createDataDirectory() {
        File dir = new File(DATA_DIRECTORY);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    // Adaptadores para Gson
    private static class LocalDateTimeAdapter implements JsonSerializer<LocalDateTime>, JsonDeserializer<LocalDateTime> {
        @Override
        public JsonElement serialize(LocalDateTime localDateTime, Type type, JsonSerializationContext context) {
            return new JsonPrimitive(localDateTime.format(DATE_FORMATTER));
        }

        @Override
        public LocalDateTime deserialize(JsonElement json, Type type, JsonDeserializationContext context) {
            return LocalDateTime.parse(json.getAsString(), DATE_FORMATTER);
        }
    }

    private static class BigDecimalAdapter implements JsonSerializer<BigDecimal>, JsonDeserializer<BigDecimal> {
        @Override
        public JsonElement serialize(BigDecimal bigDecimal, Type type, JsonSerializationContext context) {
            return new JsonPrimitive(bigDecimal.toString());
        }

        @Override
        public BigDecimal deserialize(JsonElement json, Type type, JsonDeserializationContext context) {
            return new BigDecimal(json.getAsString());
        }
    }
}
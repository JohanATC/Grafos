// DataGenerator.java
package com.banking.util;

import com.banking.model.Account;
import com.banking.model.Transaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class DataGenerator {
    private static final String[] BANK_NAMES = {
            "Banco Pichincha", "Banco del Pacífico", "Banco de Guayaquil",
            "Banco Internacional", "Banco Bolivariano", "Banco del Austro",
            "Banco ProCredit", "Banco Solidario", "Banco Machala"
    };

    private static final String[] FIRST_NAMES = {
            "María", "Juan", "Ana", "Carlos", "Lucía", "Miguel", "Carmen", "José",
            "Patricia", "Francisco", "Isabel", "Antonio", "Rosa", "Manuel", "Elena",
            "Luis", "Dolores", "Jesús", "Pilar", "Javier", "Teresa", "Fernando"
    };

    private static final String[] LAST_NAMES = {
            "García", "Rodríguez", "González", "Fernández", "López", "Martínez",
            "Sánchez", "Pérez", "Gómez", "Martín", "Jiménez", "Ruiz", "Hernández",
            "Díaz", "Moreno", "Álvarez", "Muñoz", "Romero", "Alonso", "Gutiérrez"
    };

    private static final String[] TRANSACTION_TYPES = {
            "TRANSFERENCIA", "PAGO_SERVICIOS", "NOMINA", "DEPOSITO",
            "RETIRO", "COMPRA", "PRESTAMO", "INVERSION"
    };

    private final Random random;

    public DataGenerator() {
        this.random = new Random();
    }

    public DataGenerator(long seed) {
        this.random = new Random(seed);
    }

    // Generación de cuentas
    public List<Account> generateAccounts(int count) {
        List<Account> accounts = new ArrayList<>();
        Set<String> usedAccountNumbers = new HashSet<>();

        for (int i = 0; i < count; i++) {
            String accountId = "ACC" + String.format("%06d", i + 1);
            String accountNumber;

            // Generar número de cuenta único
            do {
                accountNumber = generateAccountNumber();
            } while (usedAccountNumbers.contains(accountNumber));

            usedAccountNumbers.add(accountNumber);

            String ownerName = generateRandomName();
            String bankName = BANK_NAMES[random.nextInt(BANK_NAMES.length)];
            BigDecimal balance = generateRandomBalance();

            accounts.add(new Account(accountId, accountNumber, ownerName, bankName, balance));
        }

        return accounts;
    }

    // Generación de transacciones realistas
    public List<Transaction> generateRealisticTransactions(List<Account> accounts, int transactionCount) {
        List<Transaction> transactions = new ArrayList<>();
        LocalDateTime baseDate = LocalDateTime.now().minus(365, ChronoUnit.DAYS);

        // Crear algunos patrones de transacciones más realistas
        Map<Account, List<Account>> frequentConnections = createFrequentConnections(accounts);

        for (int i = 0; i < transactionCount; i++) {
            Account sourceAccount = accounts.get(random.nextInt(accounts.size()));
            Account destinationAccount;

            // 70% de posibilidad de usar conexiones frecuentes, 30% aleatorio
            if (random.nextDouble() < 0.7 && frequentConnections.containsKey(sourceAccount)
                    && !frequentConnections.get(sourceAccount).isEmpty()) {
                List<Account> connections = frequentConnections.get(sourceAccount);
                destinationAccount = connections.get(random.nextInt(connections.size()));
            } else {
                do {
                    destinationAccount = accounts.get(random.nextInt(accounts.size()));
                } while (destinationAccount.equals(sourceAccount));
            }

            String transactionId = "TXN" + String.format("%08d", i + 1);
            BigDecimal amount = generateRealisticTransactionAmount();
            String transactionType = TRANSACTION_TYPES[random.nextInt(TRANSACTION_TYPES.length)];
            String description = generateTransactionDescription(transactionType);

            // Crear transacción con fecha aleatoria en el último año
            Transaction transaction = new Transaction(
                    transactionId, sourceAccount, destinationAccount,
                    amount, description, transactionType
            );

            // Simular fecha aleatoria
            LocalDateTime transactionDate = baseDate.plus(
                    random.nextInt(365), ChronoUnit.DAYS
            ).plus(
                    random.nextInt(24), ChronoUnit.HOURS
            ).plus(
                    random.nextInt(60), ChronoUnit.MINUTES
            );

            transactions.add(transaction);
        }

        return transactions;
    }

    // Generar transacciones con patrones temporales
    public List<Transaction> generateTransactionsWithTimePatterns(List<Account> accounts,
                                                                  int daysBack,
                                                                  int transactionsPerDay) {
        List<Transaction> transactions = new ArrayList<>();
        LocalDateTime startDate = LocalDateTime.now().minus(daysBack, ChronoUnit.DAYS);

        int transactionCounter = 1;

        for (int day = 0; day < daysBack; day++) {
            LocalDateTime dayStart = startDate.plus(day, ChronoUnit.DAYS);

            // Simular patrones de actividad (más transacciones en días laborables)
            int dailyTransactions = transactionsPerDay;
            if (dayStart.getDayOfWeek().getValue() >= 6) { // Fin de semana
                dailyTransactions = (int) (transactionsPerDay * 0.3);
            }

            for (int t = 0; t < dailyTransactions; t++) {
                Account source = accounts.get(random.nextInt(accounts.size()));
                Account destination;
                do {
                    destination = accounts.get(random.nextInt(accounts.size()));
                } while (destination.equals(source));

                String transactionId = "TXN" + String.format("%08d", transactionCounter++);
                BigDecimal amount = generateRealisticTransactionAmount();
                String transactionType = TRANSACTION_TYPES[random.nextInt(TRANSACTION_TYPES.length)];
                String description = generateTransactionDescription(transactionType);

                transactions.add(new Transaction(
                        transactionId, source, destination, amount, description, transactionType
                ));
            }
        }

        return transactions;
    }

    // Métodos auxiliares privados
    private String generateAccountNumber() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }

    private String generateRandomName() {
        String firstName = FIRST_NAMES[random.nextInt(FIRST_NAMES.length)];
        String lastName1 = LAST_NAMES[random.nextInt(LAST_NAMES.length)];
        String lastName2 = LAST_NAMES[random.nextInt(LAST_NAMES.length)];
        return firstName + " " + lastName1 + " " + lastName2;
    }

    private BigDecimal generateRandomBalance() {
        // Balance entre $100 y $50,000
        double balance = 100 + (random.nextDouble() * 49900);
        return BigDecimal.valueOf(balance).setScale(2, BigDecimal.ROUND_HALF_UP);
    }

    private BigDecimal generateRealisticTransactionAmount() {
        // Generar montos más realistas con distribución logarítmica
        double rand = random.nextDouble();
        double amount;

        if (rand < 0.5) {
            // 50% - Transacciones pequeñas ($1 - $100)
            amount = 1 + (random.nextDouble() * 99);
        } else if (rand < 0.8) {
            // 30% - Transacciones medianas ($100 - $1,000)
            amount = 100 + (random.nextDouble() * 900);
        } else if (rand < 0.95) {
            // 15% - Transacciones grandes ($1,000 - $10,000)
            amount = 1000 + (random.nextDouble() * 9000);
        } else {
            // 5% - Transacciones muy grandes ($10,000 - $50,000)
            amount = 10000 + (random.nextDouble() * 40000);
        }

        return BigDecimal.valueOf(amount).setScale(2, BigDecimal.ROUND_HALF_UP);
    }

    private String generateTransactionDescription(String transactionType) {
        switch (transactionType) {
            case "TRANSFERENCIA":
                return "Transferencia bancaria";
            case "PAGO_SERVICIOS":
                return "Pago de servicios básicos";
            case "NOMINA":
                return "Pago de nómina";
            case "DEPOSITO":
                return "Depósito en cuenta";
            case "RETIRO":
                return "Retiro de efectivo";
            case "COMPRA":
                return "Pago por compra";
            case "PRESTAMO":
                return "Pago de préstamo";
            case "INVERSION":
                return "Inversión financiera";
            default:
                return "Transacción bancaria";
        }
    }

    private Map<Account, List<Account>> createFrequentConnections(List<Account> accounts) {
        Map<Account, List<Account>> connections = new HashMap<>();

        for (Account account : accounts) {
            List<Account> frequentDestinations = new ArrayList<>();

            // Cada cuenta tiene entre 1 y 5 destinos frecuentes
            int connectionCount = 1 + random.nextInt(5);
            Set<Account> selectedAccounts = new HashSet<>();

            for (int i = 0; i < connectionCount && selectedAccounts.size() < accounts.size() - 1; i++) {
                Account destination;
                do {
                    destination = accounts.get(random.nextInt(accounts.size()));
                } while (destination.equals(account) || selectedAccounts.contains(destination));

                selectedAccounts.add(destination);
                frequentDestinations.add(destination);
            }

            connections.put(account, frequentDestinations);
        }

        return connections;
    }
}

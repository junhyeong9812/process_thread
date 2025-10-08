package com.study.common.util;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 테스트 데이터 생성 유틸리티 클래스
 * 다양한 형태의 랜덤 데이터를 생성합니다.
 */
public class RandomDataGenerator {

    private static final Random random = new Random();
    private static final String ALPHANUMERIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final String NUMBERS = "0123456789";

    /**
     * 랜덤 정수 생성 (0 ~ max-1)
     */
    public static int randomInt(int max) {
        return ThreadLocalRandom.current().nextInt(max);
    }

    /**
     * 랜덤 정수 생성 (min ~ max-1)
     */
    public static int randomInt(int min, int max) {
        return ThreadLocalRandom.current().nextInt(min, max);
    }

    /**
     * 랜덤 long 생성
     */
    public static long randomLong(long min, long max) {
        return ThreadLocalRandom.current().nextLong(min, max);
    }

    /**
     * 랜덤 double 생성 (0.0 ~ 1.0)
     */
    public static double randomDouble() {
        return ThreadLocalRandom.current().nextDouble();
    }

    /**
     * 랜덤 double 생성 (min ~ max)
     */
    public static double randomDouble(double min, double max) {
        return ThreadLocalRandom.current().nextDouble(min, max);
    }

    /**
     * 랜덤 boolean 생성
     */
    public static boolean randomBoolean() {
        return ThreadLocalRandom.current().nextBoolean();
    }

    /**
     * 랜덤 문자열 생성 (영문자 + 숫자)
     */
    public static String randomString(int length) {
        return randomString(length, ALPHANUMERIC);
    }

    /**
     * 랜덤 문자열 생성 (지정된 문자셋)
     */
    public static String randomString(int length, String charset) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(charset.charAt(randomInt(charset.length())));
        }
        return sb.toString();
    }

    /**
     * 랜덤 영문자 문자열 생성
     */
    public static String randomAlphabet(int length) {
        return randomString(length, ALPHABET);
    }

    /**
     * 랜덤 숫자 문자열 생성
     */
    public static String randomNumericString(int length) {
        return randomString(length, NUMBERS);
    }

    /**
     * 랜덤 UUID 생성
     */
    public static String randomUUID() {
        return UUID.randomUUID().toString();
    }

    /**
     * 랜덤 이메일 생성
     */
    public static String randomEmail() {
        String username = randomAlphabet(8).toLowerCase();
        String[] domains = {"gmail.com", "yahoo.com", "outlook.com", "test.com"};
        String domain = domains[randomInt(domains.length)];
        return username + "@" + domain;
    }

    /**
     * 랜덤 전화번호 생성 (010-XXXX-XXXX)
     */
    public static String randomPhoneNumber() {
        return String.format("010-%04d-%04d", randomInt(10000), randomInt(10000));
    }

    /**
     * 랜덤 날짜 생성 (현재 기준 ± days)
     */
    public static Date randomDate(int daysRange) {
        long now = System.currentTimeMillis();
        long randomMillis = randomLong(-daysRange * 86400000L, daysRange * 86400000L);
        return new Date(now + randomMillis);
    }

    /**
     * 랜덤 정수 배열 생성
     */
    public static int[] randomIntArray(int size, int min, int max) {
        return IntStream.range(0, size)
                .map(i -> randomInt(min, max))
                .toArray();
    }

    /**
     * 랜덤 정수 리스트 생성
     */
    public static List<Integer> randomIntList(int size, int min, int max) {
        return IntStream.range(0, size)
                .map(i -> randomInt(min, max))
                .boxed()
                .collect(Collectors.toList());
    }

    /**
     * 랜덤 double 배열 생성
     */
    public static double[] randomDoubleArray(int size, double min, double max) {
        double[] array = new double[size];
        for (int i = 0; i < size; i++) {
            array[i] = randomDouble(min, max);
        }
        return array;
    }

    /**
     * 랜덤 문자열 리스트 생성
     */
    public static List<String> randomStringList(int size, int stringLength) {
        List<String> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            list.add(randomString(stringLength));
        }
        return list;
    }

    /**
     * 리스트에서 랜덤 요소 선택
     */
    public static <T> T randomElement(List<T> list) {
        if (list == null || list.isEmpty()) {
            throw new IllegalArgumentException("List is empty");
        }
        return list.get(randomInt(list.size()));
    }

    /**
     * 배열에서 랜덤 요소 선택
     */
    @SafeVarargs
    public static <T> T randomElement(T... array) {
        if (array == null || array.length == 0) {
            throw new IllegalArgumentException("Array is empty");
        }
        return array[randomInt(array.length)];
    }

    /**
     * 리스트 셔플 (Fisher-Yates 알고리즘)
     */
    public static <T> List<T> shuffle(List<T> list) {
        List<T> shuffled = new ArrayList<>(list);
        Collections.shuffle(shuffled);
        return shuffled;
    }

    /**
     * 랜덤 바이트 배열 생성
     */
    public static byte[] randomBytes(int size) {
        byte[] bytes = new byte[size];
        ThreadLocalRandom.current().nextBytes(bytes);
        return bytes;
    }

    /**
     * 가우시안(정규분포) 랜덤 double 생성
     */
    public static double randomGaussian(double mean, double stdDev) {
        return mean + ThreadLocalRandom.current().nextGaussian() * stdDev;
    }

    /**
     * 랜덤 작업 데이터 생성 (벤치마크용)
     */
    public static class TaskData {
        private final String id;
        private final String name;
        private final int priority;
        private final long duration;
        private final Map<String, Object> metadata;

        public TaskData(String id, String name, int priority, long duration) {
            this.id = id;
            this.name = name;
            this.priority = priority;
            this.duration = duration;
            this.metadata = new HashMap<>();
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public int getPriority() { return priority; }
        public long getDuration() { return duration; }
        public Map<String, Object> getMetadata() { return metadata; }

        @Override
        public String toString() {
            return String.format("TaskData{id='%s', name='%s', priority=%d, duration=%d}",
                    id, name, priority, duration);
        }
    }

    /**
     * 랜덤 TaskData 생성
     */
    public static TaskData randomTaskData() {
        String id = randomUUID();
        String name = "Task-" + randomAlphabet(6);
        int priority = randomInt(1, 11);
        long duration = randomLong(10, 1000);
        return new TaskData(id, name, priority, duration);
    }

    /**
     * 랜덤 TaskData 리스트 생성
     */
    public static List<TaskData> randomTaskDataList(int size) {
        List<TaskData> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            list.add(randomTaskData());
        }
        return list;
    }

    /**
     * CPU 집약적 작업을 위한 랜덤 데이터 생성
     */
    public static int[][] randomMatrix(int rows, int cols, int min, int max) {
        int[][] matrix = new int[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                matrix[i][j] = randomInt(min, max);
            }
        }
        return matrix;
    }

    /**
     * 랜덤 Map 생성
     */
    public static Map<String, Object> randomMap(int size) {
        Map<String, Object> map = new HashMap<>();
        for (int i = 0; i < size; i++) {
            String key = "key_" + randomAlphabet(5);
            Object value = switch (randomInt(4)) {
                case 0 -> randomInt(1000);
                case 1 -> randomString(10);
                case 2 -> randomDouble();
                default -> randomBoolean();
            };
            map.put(key, value);
        }
        return map;
    }

    /**
     * 랜덤 Person 데이터 생성 (테스트용)
     */
    public static class Person {
        private final String name;
        private final int age;
        private final String email;
        private final String phone;

        public Person(String name, int age, String email, String phone) {
            this.name = name;
            this.age = age;
            this.email = email;
            this.phone = phone;
        }

        public String getName() { return name; }
        public int getAge() { return age; }
        public String getEmail() { return email; }
        public String getPhone() { return phone; }

        @Override
        public String toString() {
            return String.format("Person{name='%s', age=%d, email='%s', phone='%s'}",
                    name, age, email, phone);
        }
    }

    /**
     * 랜덤 Person 생성
     */
    public static Person randomPerson() {
        String[] firstNames = {"John", "Jane", "Mike", "Sarah", "David", "Emma"};
        String[] lastNames = {"Smith", "Johnson", "Brown", "Lee", "Kim", "Park"};

        String name = randomElement(firstNames) + " " + randomElement(lastNames);
        int age = randomInt(20, 70);
        String email = randomEmail();
        String phone = randomPhoneNumber();

        return new Person(name, age, email, phone);
    }

    /**
     * 랜덤 Person 리스트 생성
     */
    public static List<Person> randomPersonList(int size) {
        List<Person> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            list.add(randomPerson());
        }
        return list;
    }

    /**
     * Seed 설정 (재현 가능한 테스트용)
     */
    public static void setSeed(long seed) {
        random.setSeed(seed);
    }

    /**
     * 테스트 메인 메서드
     */
    public static void main(String[] args) {
        System.out.println("=== RandomDataGenerator 테스트 ===\n");

        // 1. 기본 타입
        System.out.println("1. 기본 타입:");
        System.out.println("Random Int (0-100): " + randomInt(100));
        System.out.println("Random Int (50-100): " + randomInt(50, 100));
        System.out.println("Random Long: " + randomLong(1000L, 9999L));
        System.out.println("Random Double: " + randomDouble());
        System.out.println("Random Boolean: " + randomBoolean());

        // 2. 문자열
        System.out.println("\n2. 문자열:");
        System.out.println("Random String: " + randomString(10));
        System.out.println("Random Alphabet: " + randomAlphabet(8));
        System.out.println("Random Numeric: " + randomNumericString(6));
        System.out.println("Random UUID: " + randomUUID());
        System.out.println("Random Email: " + randomEmail());
        System.out.println("Random Phone: " + randomPhoneNumber());

        // 3. 컬렉션
        System.out.println("\n3. 컬렉션:");
        System.out.println("Random Int Array: " + Arrays.toString(randomIntArray(5, 1, 100)));
        System.out.println("Random Int List: " + randomIntList(5, 1, 100));
        System.out.println("Random String List: " + randomStringList(3, 5));

        // 4. TaskData
        System.out.println("\n4. TaskData:");
        TaskData task = randomTaskData();
        System.out.println(task);
        System.out.println("TaskData List:");
        randomTaskDataList(3).forEach(System.out::println);

        // 5. Person
        System.out.println("\n5. Person:");
        Person person = randomPerson();
        System.out.println(person);
        System.out.println("Person List:");
        randomPersonList(3).forEach(System.out::println);

        // 6. 고급 기능
        System.out.println("\n6. 고급 기능:");
        System.out.println("Random Element: " + randomElement(List.of("A", "B", "C", "D")));
        System.out.println("Random Gaussian: " + randomGaussian(100, 15));
        System.out.println("Random Map: " + randomMap(3));
    }
}
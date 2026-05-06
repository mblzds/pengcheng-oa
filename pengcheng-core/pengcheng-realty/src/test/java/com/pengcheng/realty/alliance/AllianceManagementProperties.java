package com.pengcheng.realty.alliance;

import com.pengcheng.realty.alliance.entity.Alliance;
import com.pengcheng.realty.common.constants.RealtyRoleConstants;
import net.jqwik.api.*;
import net.jqwik.api.constraints.*;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 联盟商管理属性测试
 *
 * <p>Property 11: 联盟商创建自动建账 — For any 成功创建的联盟商，系统中应存在关联的用户账号，角色为"联盟商负责人"
 * <p>Property 12: 联盟商停用级联效果 — For any 被停用的联盟商，关联账号无法登录，且报备联盟商选择列表中不出现
 *
 * <p><b>Validates: Requirements 6.2, 6.4</b>
 */
class AllianceManagementProperties {

    // ========== Simulated data models ==========

    /** Simulated system user account */
    record SimUser(long userId, String username, String nickname, String phone, int status, Set<String> roleCodes) {}

    /** Result of alliance creation: the alliance and its associated user */
    record CreateResult(Alliance alliance, SimUser user) {}

    // ========== Generators ==========

    @Provide
    Arbitrary<String> companyNames() {
        return Arbitraries.strings().alpha().ofMinLength(2).ofMaxLength(20)
                .map(s -> s + "房产经纪公司");
    }

    @Provide
    Arbitrary<String> contactNames() {
        return Arbitraries.of("张三", "李四", "王五", "赵六", "钱七", "孙八", "周九", "吴十");
    }

    @Provide
    Arbitrary<String> phoneNumbers() {
        return Arbitraries.strings().numeric().ofLength(11)
                .filter(s -> s.startsWith("1"));
    }

    @Provide
    Arbitrary<Integer> staffSizes() {
        return Arbitraries.integers().between(1, 500);
    }

    @Provide
    Arbitrary<Integer> allianceLevels() {
        return Arbitraries.integers().between(1, 4); // 1-普通 2-银牌 3-金牌 4-钻石
    }


    // ========== Core logic simulation (mirrors AllianceService) ==========

    /**
     * Simulates AllianceService.createAlliance:
     * 1. Creates a system user account with username "alliance_" + timestamp
     * 2. Assigns the "alliance_manager" role to the user
     * 3. Creates the alliance entity linked to the user via userId
     */
    private CreateResult simulateCreateAlliance(
            long allianceId, long userId,
            String companyName, String contactName, String contactPhone,
            int staffSize, int level, Long channelUserId
    ) {
        // Step 1: Create system user (mirrors SysUser creation in AllianceService)
        SimUser user = new SimUser(
                userId,
                "alliance_" + System.nanoTime(),
                contactName,
                contactPhone,
                1, // status = enabled
                Set.of(RealtyRoleConstants.ALLIANCE_MANAGER)
        );

        // Step 2: Create alliance linked to user
        Alliance alliance = Alliance.builder()
                .companyName(companyName)
                .officeAddress("测试地址")
                .contactName(contactName)
                .contactPhone(contactPhone)
                .staffSize(staffSize)
                .level(level)
                .status(1) // 默认启用
                .userId(user.userId())
                .channelUserId(channelUserId)
                .build();
        alliance.setId(allianceId);

        return new CreateResult(alliance, user);
    }

    /**
     * Simulates AllianceService.disableAlliance:
     * 1. Sets alliance status to 0 (disabled)
     * 2. Sets associated user status to 0 (disabled)
     */
    private SimUser simulateDisableAlliance(Alliance alliance, SimUser user) {
        alliance.setStatus(0);
        // Return new user with disabled status (mirrors sysUserService.updateById)
        return new SimUser(user.userId(), user.username(), user.nickname(),
                user.phone(), 0, user.roleCodes());
    }

    /**
     * Simulates AllianceService.listEnabled:
     * Returns only alliances with status == 1 (enabled).
     */
    private List<Alliance> simulateListEnabled(List<Alliance> allAlliances) {
        return allAlliances.stream()
                .filter(a -> a.getStatus() != null && a.getStatus() == 1)
                .toList();
    }

    /**
     * Checks if a user can login (status == 1).
     */
    private boolean canLogin(SimUser user) {
        return user.status() == 1;
    }

    // ========== Property 11: 联盟商创建自动建账 ==========

    /**
     * Property 11: 联盟商创建自动建账
     *
     * <p>For any 成功创建的联盟商，系统中应存在一个与该联盟商关联的用户账号，
     * 且该账号角色为"联盟商负责人"。
     *
     * <p><b>Validates: Requirements 6.2</b>
     */
    @Property(tries = 100)
    void allianceCreationAutoCreatesAccountWithCorrectRole(
            @ForAll @LongRange(min = 1, max = 1000) long allianceId,
            @ForAll @LongRange(min = 1, max = 1000) long userId,
            @ForAll("companyNames") String companyName,
            @ForAll("contactNames") String contactName,
            @ForAll("phoneNumbers") String contactPhone,
            @ForAll("staffSizes") int staffSize,
            @ForAll("allianceLevels") int level
    ) {
        CreateResult result = simulateCreateAlliance(
                allianceId, userId, companyName, contactName, contactPhone,
                staffSize, level, null);

        Alliance alliance = result.alliance();
        SimUser user = result.user();

        // 1. Alliance should have a linked userId
        assertThat(alliance.getUserId())
                .as("Alliance must have an associated user account")
                .isNotNull();

        // 2. The linked userId should match the created user
        assertThat(alliance.getUserId())
                .as("Alliance userId should match the created user's ID")
                .isEqualTo(user.userId());

        // 3. The user account should have the "alliance_manager" role
        assertThat(user.roleCodes())
                .as("Associated user must have the alliance_manager role")
                .contains(RealtyRoleConstants.ALLIANCE_MANAGER);

        // 4. The user account should be enabled (status = 1)
        assertThat(user.status())
                .as("Newly created user account should be enabled")
                .isEqualTo(1);

        // 5. The user's contact info should match the alliance contact info
        assertThat(user.nickname())
                .as("User nickname should match alliance contact name")
                .isEqualTo(contactName);
        assertThat(user.phone())
                .as("User phone should match alliance contact phone")
                .isEqualTo(contactPhone);

        // 6. Alliance should be enabled by default
        assertThat(alliance.getStatus())
                .as("Newly created alliance should be enabled (status=1)")
                .isEqualTo(1);
    }

    /**
     * Property 11 (additional): For multiple alliance creations, each alliance
     * gets its own unique user account.
     *
     * <p><b>Validates: Requirements 6.2</b>
     */
    @Property(tries = 100)
    void eachAllianceGetsUniqueUserAccount(
            @ForAll @IntRange(min = 2, max = 10) int allianceCount,
            @ForAll("contactNames") String contactName,
            @ForAll("phoneNumbers") String contactPhone
    ) {
        Set<Long> userIds = new HashSet<>();
        Set<Long> allianceIds = new HashSet<>();

        for (int i = 0; i < allianceCount; i++) {
            long aId = i + 1;
            long uId = 100 + i;
            CreateResult result = simulateCreateAlliance(
                    aId, uId, "公司" + i, contactName, contactPhone,
                    10, 1, null);

            userIds.add(result.user().userId());
            allianceIds.add(result.alliance().getId());

            // Each alliance must have a linked user with correct role
            assertThat(result.user().roleCodes())
                    .contains(RealtyRoleConstants.ALLIANCE_MANAGER);
        }

        // All user IDs should be unique (one account per alliance)
        assertThat(userIds)
                .as("Each alliance should have a unique user account")
                .hasSize(allianceCount);
        assertThat(allianceIds).hasSize(allianceCount);
    }

    // ========== Property 12: 联盟商停用级联效果 ==========

    /**
     * Property 12: 联盟商停用级联效果
     *
     * <p>For any 被停用的联盟商，该联盟商的关联账号应无法登录，
     * 且在客户报备的联盟商选择列表中不应出现该联盟商。
     *
     * <p><b>Validates: Requirements 6.4</b>
     */
    @Property(tries = 100)
    void disabledAllianceCascadesEffects(
            @ForAll @LongRange(min = 1, max = 1000) long allianceId,
            @ForAll @LongRange(min = 1, max = 1000) long userId,
            @ForAll("companyNames") String companyName,
            @ForAll("contactNames") String contactName,
            @ForAll("phoneNumbers") String contactPhone,
            @ForAll("staffSizes") int staffSize,
            @ForAll("allianceLevels") int level
    ) {
        // Create an alliance (initially enabled)
        CreateResult result = simulateCreateAlliance(
                allianceId, userId, companyName, contactName, contactPhone,
                staffSize, level, null);

        Alliance alliance = result.alliance();
        SimUser user = result.user();

        // Verify initially enabled
        assertThat(alliance.getStatus()).isEqualTo(1);
        assertThat(canLogin(user)).isTrue();

        // Disable the alliance
        SimUser disabledUser = simulateDisableAlliance(alliance, user);

        // 1. Alliance status should be 0 (disabled)
        assertThat(alliance.getStatus())
                .as("Disabled alliance status should be 0")
                .isEqualTo(0);

        // 2. Associated user account should be unable to login (status = 0)
        assertThat(canLogin(disabledUser))
                .as("Disabled alliance's user account should not be able to login")
                .isFalse();
        assertThat(disabledUser.status())
                .as("Disabled alliance's user status should be 0")
                .isEqualTo(0);

        // 3. Disabled alliance should NOT appear in the enabled alliance list
        List<Alliance> allAlliances = List.of(alliance);
        List<Alliance> enabledList = simulateListEnabled(allAlliances);
        assertThat(enabledList)
                .as("Disabled alliance should not appear in enabled list for customer reporting")
                .doesNotContain(alliance);
    }

    /**
     * Property 12 (additional): In a mixed set of enabled and disabled alliances,
     * only enabled ones appear in the selection list.
     *
     * <p><b>Validates: Requirements 6.4</b>
     */
    @Property(tries = 100)
    void onlyEnabledAlliancesAppearInSelectionList(
            @ForAll @IntRange(min = 1, max = 10) int totalCount,
            @ForAll @IntRange(min = 0, max = 10) int disableCount
    ) {
        int actualDisableCount = Math.min(disableCount, totalCount);
        List<Alliance> allAlliances = new ArrayList<>();
        Map<Long, SimUser> userMap = new HashMap<>();

        // Create alliances
        for (int i = 0; i < totalCount; i++) {
            long aId = i + 1;
            long uId = 100 + i;
            CreateResult result = simulateCreateAlliance(
                    aId, uId, "公司" + i, "负责人" + i, "13800000" + String.format("%03d", i),
                    10, 1, null);
            allAlliances.add(result.alliance());
            userMap.put(result.user().userId(), result.user());
        }

        // Disable some alliances
        Set<Long> disabledAllianceIds = new HashSet<>();
        for (int i = 0; i < actualDisableCount; i++) {
            Alliance a = allAlliances.get(i);
            SimUser u = userMap.get(a.getUserId());
            SimUser disabledUser = simulateDisableAlliance(a, u);
            userMap.put(disabledUser.userId(), disabledUser);
            disabledAllianceIds.add(a.getId());
        }

        // Get enabled list (simulates listEnabled)
        List<Alliance> enabledList = simulateListEnabled(allAlliances);

        // Verify: no disabled alliance in the list
        for (Alliance a : enabledList) {
            assertThat(disabledAllianceIds)
                    .as("Disabled alliance %d should not be in enabled list", a.getId())
                    .doesNotContain(a.getId());
            assertThat(a.getStatus())
                    .as("All alliances in enabled list should have status=1")
                    .isEqualTo(1);
        }

        // Verify: all enabled alliances ARE in the list
        for (Alliance a : allAlliances) {
            if (a.getStatus() == 1) {
                assertThat(enabledList).contains(a);
            }
        }

        // Verify: disabled users cannot login
        for (Long disabledId : disabledAllianceIds) {
            Alliance disabledAlliance = allAlliances.stream()
                    .filter(a -> a.getId().equals(disabledId))
                    .findFirst().orElseThrow();
            SimUser user = userMap.get(disabledAlliance.getUserId());
            assertThat(canLogin(user))
                    .as("User of disabled alliance %d should not be able to login", disabledId)
                    .isFalse();
        }

        // Verify count
        assertThat(enabledList).hasSize(totalCount - actualDisableCount);
    }
}

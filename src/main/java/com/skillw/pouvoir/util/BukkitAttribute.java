package com.skillw.pouvoir.util;

import org.bukkit.attribute.Attribute;
import taboolib.common5.Coerce;
import taboolib.library.reflex.ClassField;
import taboolib.library.reflex.ClassMethod;
import taboolib.library.reflex.ReflexClass;
import taboolib.module.nms.MinecraftVersion;

import java.util.Arrays;

import static taboolib.module.nms.MinecraftServerUtilKt.nmsClass;

/**
 * Attribute 映射类
 *
 * @author sky
 * @since 2019-12-11 19:31
 */
public enum BukkitAttribute {

    /**
     * 最大生命值
     */
    MAX_HEALTH("generic.maxHealth", new String[]{"health", "maxHealth"}),

    /**
     * 最大跟随距离
     */
    FOLLOW_RANGE("generic.followRange", new String[]{"follow", "followRange"}),

    /**
     * 击退抗性
     */
    KNOCKBACK_RESISTANCE("generic.knockbackResistance", new String[]{"knockback", "knockbackResistance"}),

    /**
     * 移动速度
     */
    MOVEMENT_SPEED("generic.movementSpeed", new String[]{"speed", "movementSpeed", "walkSpeed"}),

    /**
     * 飞行速度
     */
    FLYING_SPEED("generic.flyingSpeed", new String[]{"flySpeed", "flyingSpeed"}),

    /**
     * 攻击力
     */
    ATTACK_DAMAGE("generic.attackDamage", new String[]{"damage", "attackDamage"}),

    /**
     * 击退
     */
    ATTACK_KNOCKBACK("generic.attackKnockback", new String[]{"damageKnockback", "attackKnockback"}),

    /**
     * 攻速
     */
    ATTACK_SPEED("generic.attackSpeed", new String[]{"damageSpeed", "attackSpeed"}),

    /**
     * 护甲
     */
    ARMOR("generic.armor", new String[]{"armor"}),

    /**
     * 护甲韧性
     */
    ARMOR_TOUGHNESS("generic.armorToughness", new String[]{"toughness", "armorToughness"}),

    /**
     * 幸运
     */
    LUCK("generic.luck", new String[]{"luck"});

    final String minecraftKey;
    final String[] simplifiedKey;

    BukkitAttribute(String minecraftKey, String[] simplifiedKey) {
        this.minecraftKey = minecraftKey;
        this.simplifiedKey = simplifiedKey;
    }

    public static BukkitAttribute parse(String source) {
        return Arrays.stream(values()).filter(attribute -> attribute.match(source)).findFirst().orElse(null);
    }

    public String getMinecraftKey() {
        return this.minecraftKey;
    }

    public String[] getSimplifiedKey() {
        return this.simplifiedKey;
    }

    public Attribute toBukkit() {
        Attribute attribute = Coerce.toEnum("GENERIC_" + this.name(), Attribute.class);
        if (attribute == null) {
            attribute = Coerce.toEnum(this.name(), Attribute.class);
        }
        return attribute;
    }

    @SuppressWarnings("ConstantConditions")
    public Object toNMS() {
        ReflexClass genericAttributesClass = ReflexClass.Companion.of(nmsClass("GenericAttributes"), false);
        ReflexClass attributeBaseClass = ReflexClass.Companion.of(nmsClass("AttributeBase"), false);
        if (MinecraftVersion.INSTANCE.getMajorLegacy() <= 11300) {
            ClassMethod method = attributeBaseClass.getMethod("getName", false, false);
            for (ClassField classField : genericAttributesClass.getStructure().getFields()) {
                Object attribute = classField.get(null);
                if (method.invoke(attributeBaseClass.getStructure().getOwner().cast(attribute)).equals(this.minecraftKey)) {
                    return attribute;
                }
            }
        } else {
            return genericAttributesClass.getField(this.name(), false, false).get(null);
        }
        return null;
    }

    public boolean match(String source) {
        return this.name().equalsIgnoreCase(source) || this.minecraftKey.equalsIgnoreCase(source) || Arrays.stream(this.simplifiedKey).anyMatch(key -> key.equalsIgnoreCase(source));
    }
}
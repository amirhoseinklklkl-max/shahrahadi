package com.persiancity.game.game;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Random;

/**
 * سیستم ماموریت‌های NPC — ببر و بیار با جایزه
 */
public class MissionSystem {

    public static class Mission {
        public int giverIndex;      // اندیس NPC دهنده
        public int kind;            // ۰ = بردن بسته به ساختمان، ۱ = رساندن نامه به NPC
        public int targetBuildingType = -1;
        public int targetNpcIndex = -1;
        public int reward;
        public String title;
        public boolean done = false;

        public String targetText(World world) {
            if (kind == 0) {
                for (Building b : world.buildings) {
                    if (b.type == targetBuildingType) return b.name;
                }
                return "مقصد";
            } else {
                if (targetNpcIndex >= 0 && targetNpcIndex < world.cityNpcs.size()) {
                    return world.cityNpcs.get(targetNpcIndex).name;
                }
                return "شهروند";
            }
        }
    }

    private final ArrayList<Mission> active = new ArrayList<>();
    private final Random rnd;
    private final World world;

    public MissionSystem(World world, Random rnd) {
        this.world = world;
        this.rnd = rnd;
    }

    public ArrayList<Mission> getActive() {
        return active;
    }

    public boolean hasFreeSlots() {
        return active.size() < 3;
    }

    /**
     * آیا این NPC ماموریت تازه می‌دهد؟
     */
    public boolean canOffer(Npc npc) {
        if (npc.role != Npc.ROLE_CITIZEN) return false;
        if (!hasFreeSlots()) return false;
        for (Mission m : active) {
            if (m.giverIndex == indexOf(npc)) return false;
        }
        return rnd.nextInt(100) < 70;
    }

    private int indexOf(Npc npc) {
        return world.cityNpcs.indexOf(npc);
    }

    /**
     * ساخت ماموریت جدید از این NPC
     */
    public Mission createMission(Npc giver) {
        Mission m = new Mission();
        m.giverIndex = indexOf(giver);
        m.kind = rnd.nextInt(2);
        m.reward = 300 + rnd.nextInt(6) * 100;

        if (m.kind == 0) {
            // بردن بسته به یک ساختمان
            int[] types = {Building.CLOTHES, Building.RESTAURANT, Building.MARKET, Building.CAFE,
                    Building.CINEMA, Building.BANK, Building.SCHOOL, Building.HOSPITAL,
                    Building.TOYSTORE, Building.LIBRARY, Building.GARAGE};
            int t = types[rnd.nextInt(types.length)];
            m.targetBuildingType = t;
            for (Building b : world.buildings) {
                if (b.type == t) {
                    m.targetBuildingType = t;
                    break;
                }
            }
            m.title = "این بسته رو ببر به «" + m.targetText(world) + "»";
        } else {
            // رساندن نامه به یک شهروند دیگر
            int idx = rnd.nextInt(Math.max(1, world.cityNpcs.size()));
            if (idx == m.giverIndex) idx = (idx + 3) % Math.max(1, world.cityNpcs.size());
            m.targetNpcIndex = idx;
            m.title = "این نامه رو برسون به «" + m.targetText(world) + "»";
        }
        active.add(m);
        return m;
    }

    /**
     * اگر بازیکن نزدیک هدف ماموریت است و در دسترس، تحویل بده
     */
    public String tryComplete(Player player, Npc talkedTo) {
        for (int i = 0; i < active.size(); i++) {
            Mission m = active.get(i);
            if (m.done) continue;

            if (m.kind == 1 && talkedTo != null) {
                int idx = world.cityNpcs.indexOf(talkedTo);
                if (idx == m.targetNpcIndex) {
                    active.remove(i);
                    player.earn(m.reward);
                    return Dialogues.pick(Dialogues.MISSION_DONE_LINES, rnd) + " (+" + G.fa(m.reward) + " تومان)";
                }
            } else if (m.kind == 0 && talkedTo == null) {
                // تحویل در جلوی ساختمان مقصد
                for (Building b : world.buildings) {
                    if (b.type == m.targetBuildingType
                            && G.dist(player.x, player.y, b.doorX, b.doorY) < G.TILE * 2f) {
                        active.remove(i);
                        player.earn(m.reward);
                        return "بسته تحویل داده شد! (+" + G.fa(m.reward) + " تومان)";
                    }
                }
            }
        }
        return null;
    }

    public Mission missionNearTarget(Player player) {
        for (Mission m : active) {
            if (m.done) continue;
            if (m.kind == 0) {
                for (Building b : world.buildings) {
                    if (b.type == m.targetBuildingType
                            && G.dist(player.x, player.y, b.doorX, b.doorY) < G.TILE * 2.5f) {
                        return m;
                    }
                }
            }
        }
        return null;
    }

    /**
     * مختصات هدف اولین ماموریت فعال (برای فلش راهنما و مینی‌مپ)
     */
    public float[] firstTarget() {
        for (Mission m : active) {
            if (m.done) continue;
            if (m.kind == 0) {
                for (Building b : world.buildings) {
                    if (b.type == m.targetBuildingType) return new float[]{b.doorX, b.doorY};
                }
            } else {
                if (m.targetNpcIndex >= 0 && m.targetNpcIndex < world.cityNpcs.size()) {
                    Npc n = world.cityNpcs.get(m.targetNpcIndex);
                    return new float[]{n.x, n.y};
                }
            }
        }
        return null;
    }

    public String activeTitles() {
        if (active.isEmpty()) return null;
        StringBuilder sb = new StringBuilder("ماموریت‌ها: ");
        for (Mission m : active) {
            sb.append("• ").append(m.title).append("  ");
        }
        return sb.toString();
    }

    // ---------------- ذخیره و بارگذاری ----------------

    public JSONArray toJson() {
        JSONArray arr = new JSONArray();
        for (Mission m : active) {
            try {
                JSONObject o = new JSONObject();
                o.put("giver", m.giverIndex);
                o.put("kind", m.kind);
                o.put("bt", m.targetBuildingType);
                o.put("ni", m.targetNpcIndex);
                o.put("rw", m.reward);
                o.put("t", m.title);
                arr.put(o);
            } catch (Exception ignored) {
            }
        }
        return arr;
    }

    public void fromJson(JSONArray arr) {
        active.clear();
        if (arr == null) return;
        for (int i = 0; i < arr.length(); i++) {
            try {
                JSONObject o = arr.getJSONObject(i);
                Mission m = new Mission();
                m.giverIndex = o.optInt("giver", 0);
                m.kind = o.optInt("kind", 0);
                m.targetBuildingType = o.optInt("bt", -1);
                m.targetNpcIndex = o.optInt("ni", -1);
                m.reward = o.optInt("rw", 300);
                m.title = o.optString("t", "ماموریت");
                active.add(m);
            } catch (Exception ignored) {
            }
        }
    }
}

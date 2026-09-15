package com.funeral.config;

import com.funeral.entity.AppUser;
import com.funeral.entity.CatalogItem;
import com.funeral.entity.Resource;
import com.funeral.repo.AppUserRepository;
import com.funeral.repo.CatalogItemRepository;
import com.funeral.repo.ResourceRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** 初始化演示账号、服务/物品目录、馆内资源 */
@Component
public class DataInitializer implements CommandLineRunner {

    private final AppUserRepository userRepo;
    private final CatalogItemRepository catalogRepo;
    private final ResourceRepository resourceRepo;

    public DataInitializer(AppUserRepository userRepo, CatalogItemRepository catalogRepo,
                           ResourceRepository resourceRepo) {
        this.userRepo = userRepo;
        this.catalogRepo = catalogRepo;
        this.resourceRepo = resourceRepo;
    }

    @Override
    public void run(String... args) {
        seedUsers();
        seedCatalog();
        seedResources();
    }

    private void seedUsers() {
        if (userRepo.count() > 0) return;
        // 家属两位（用于演示亲属意见不一致）
        create("family", "family123", "张淑芬（家属·配偶）", "FAMILY");
        create("family2", "family123", "李建国（家属·长子）", "FAMILY");
        create("transport", "transport123", "王强（接运组司机）", "TRANSPORT");
        create("clerk", "clerk123", "陈晓（业务员）", "CLERK");
        create("finance", "finance123", "赵敏（财务收费）", "FINANCE");
        create("halladmin", "hall123", "孙磊（礼厅管理员）", "HALL_ADMIN");
        create("crematorium", "crema123", "周师傅（火化组）", "CREMATORIUM");
        create("leader", "leader123", "刘馆长（馆领导）", "LEADER");
    }

    private void create(String username, String pwd, String name, String role) {
        AppUser u = new AppUser();
        u.setUsername(username);
        u.setPassword(new BCryptPasswordEncoder().encode(pwd));
        u.setDisplayName(name);
        u.setRole(role);
        u.setActive(true);
        userRepo.save(u);
    }

    private void seedCatalog() {
        if (catalogRepo.count() > 0) return;
        // 公益基本服务（政府定价/指导价）
        add("PB01", "遗体接运（本县城区）", "TRANSPORT", "PUBLIC_BASIC", "180", "次", null, false,
                "县发改局核定公益接运费，城乡同价", "公益基本服务，公示定价");
        add("PB02", "遗体冷藏存放（每日）", "COLD", "PUBLIC_BASIC", "80", "日", 6, false,
                "馆内冷藏柜，按日计费", "公益基本服务，公示定价");
        add("PB03", "遗体火化（普通炉）", "CREMATION", "PUBLIC_BASIC", "300", "具", null, false,
                "普通火化炉，含骨灰装殓", "公益基本服务，公示定价");
        add("PB04", "骨灰寄存（一年）", "OTHER", "PUBLIC_BASIC", "120", "年", null, true,
                "馆内骨灰堂寄存", "公益基本服务，公示定价");

        // 自选增值服务
        add("OP01", "遗体化妆整容", "EMBALM", "OPTIONAL", "260", "具", null, true,
                "含洁面、整容、更衣辅助", "自选服务，明码标价");
        add("OP02", "高档织锦寿衣（全套）", "BURIAL_CLOTHES", "OPTIONAL", "680", "套", 20, true,
                "含寿被、鞋帽，物品可现场看样", "自选物品，按实物结算");
        add("OP03", "普通寿衣（全套）", "BURIAL_CLOTHES", "OPTIONAL", "380", "套", 30, true,
                "棉质七件套", "自选物品，按实物结算");
        add("OP04", "红木骨灰盒", "URN", "OPTIONAL", "1280", "个", 15, true,
                "实木雕花，随骨灰领取", "自选物品，按实物结算");
        add("OP05", "玉石骨灰盒", "URN", "OPTIONAL", "2600", "个", 6, true,
                "岫岩玉骨灰盒", "自选物品，按实物结算");
        add("OP06", "布质花圈", "WREATH", "OPTIONAL", "120", "个", 50, true,
                "直径1.2米，可写挽条", "自选物品，按实物结算");
        add("OP07", "鲜花花篮（一对）", "WREATH", "OPTIONAL", "300", "对", 30, true,
                "黄白菊为主", "自选物品，按实物结算");
        add("OP08", "挽联定制", "WREATH", "OPTIONAL", "60", "副", 100, true,
                "家属提供内容，当日书写", "自选物品，按实物结算");
        add("OP09", "告别仪式基础布置", "FAREWELL", "OPTIONAL", "800", "场", null, false,
                "含横幅、鲜花围棺、音响礼仪", "自选服务，明码标价");
        add("OP10", "告别仪式隆重布置", "FAREWELL", "OPTIONAL", "1800", "场", null, false,
                "含花艺背景墙、专业司仪、仪仗", "自选服务，明码标价");
        add("OP11", "家属休息室（半天）", "REST_ROOM", "OPTIONAL", "200", "间", null, true,
                "含茶水、纸巾", "自选服务，明码标价");
        add("OP12", "便民餐饮", "CATERING", "OPTIONAL", "300", "桌", null, true,
                "八菜一汤简餐，提前2小时预订", "自选服务，按桌结算");
        add("OP13", "豪华殡仪车接送", "TRANSPORT", "OPTIONAL", "500", "次", null, true,
                "凯迪拉克改装灵车", "自选服务，明码标价");
        add("OP14", "跨县长途遗体接运", "TRANSPORT", "OPTIONAL", "1600", "次", null, true,
                "含跨县接运许可办理、长途冷藏转运、司机双人轮换", "自选服务，按里程与接运许可定价");
        add("OP15", "告别厅场地使用费", "FAREWELL", "PUBLIC_BASIC", "400", "场", null, false,
                "告别厅基础场地与音响设备，按厅规格公示定价", "公益基本服务，公示定价");

        // 政府补助项目（负数冲减，家属凭证明申请）
        add("SUB01", "低保户基本服务费减免", "OTHER", "SUBSIDY", "-300", "项", null, false,
                "凭低保证减免基本火化费", "县民政局《殡葬救助实施办法》第八条");
        add("SUB02", "特困供养对象骨灰盒补贴", "URN", "SUBSIDY", "-300", "个", null, false,
                "特困人员免费提供普通骨灰盒（折抵）", "县民政局《殡葬救助实施办法》第十条");
        add("SUB03", "生态安葬奖补", "OTHER", "SUBSIDY", "-1000", "项", null, false,
                "选择骨灰撒散/深埋不留坟头", "县文明殡葬奖补政策（2024）");
    }

    private void add(String code, String name, String category, String serviceClass,
                     String price, String unit, Integer stock, boolean refundable,
                     String desc, String sourceNote) {
        CatalogItem c = new CatalogItem();
        c.setCode(code);
        c.setName(name);
        c.setCategory(category);
        c.setServiceClass(serviceClass);
        c.setUnitPrice(new BigDecimal(price));
        c.setUnit(unit);
        c.setStock(stock);
        c.setRefundable(refundable);
        c.setActive(true);
        c.setDescription(desc);
        c.setSourceNote(sourceNote);
        catalogRepo.save(c);
    }

    private void seedResources() {
        if (resourceRepo.count() > 0) return;
        // 接运车辆（含跨县接运许可与营运资质）
        vehicle("接运车 皖W·D001", "YZG-2024-001", true, true, "常驻车库，配担架与遗体袋");
        vehicle("接运车 皖W·D002", "YZG-2024-002", true, true, "常驻车库");
        vehicle("跨县长程车 皖W·D003", "YZG-2024-003", true, true, "可跑长途（外地接运），配车载冷藏");
        // 一辆资质过期车辆，用于演示车辆资质核验不通过
        vehicle("接运车 皖W·D009", "", false, false, "营运资质审验中，暂停跨县接运");

        for (int i = 1; i <= 6; i++) {
            Resource cold = new Resource();
            cold.setType("COLD");
            cold.setName("冷藏位 C-" + String.format("%02d", i));
            cold.setCapacity(1);
            cold.setAvailable(true);
            cold.setNote("单具独立冷柜，温度 -5℃，可长途遗体入库");
            resourceRepo.save(cold);
        }

        hall("明德厅", "SMALL", 30, true, "小型告别厅，约容纳30人");
        hall("怀远厅", "MEDIUM", 80, true, "中型告别厅，约容纳80人");
        hall("思亲厅", "LARGE", 150, true, "大型告别厅，约容纳150人");
        hall("千秋殿", "GRAND", 300, true, "特级告别厅，约容纳300人");

        r("FURNACE", "1号火化炉", null, true, "普通炉");
        r("FURNACE", "2号火化炉", null, true, "高档炉");
        // 3号炉初始即为检修状态，用于演示"火化设备检修"协同
        Resource maint = new Resource();
        maint.setType("FURNACE");
        maint.setName("3号火化炉");
        maint.setAvailable(false);
        maint.setNote("炉衬检修中，暂停排期");
        maint.setUnavailableUntil(LocalDateTime.now().plusDays(3));
        resourceRepo.save(maint);
    }

    private void vehicle(String name, String permitNo, boolean qualified, boolean available, String note) {
        Resource res = new Resource();
        res.setType("VEHICLE");
        res.setName(name);
        res.setPermitNo(permitNo);
        res.setQualified(qualified);
        res.setAvailable(available);
        res.setNote(note);
        resourceRepo.save(res);
    }

    private void hall(String name, String spec, int capacity, boolean available, String note) {
        Resource res = new Resource();
        res.setType("HALL");
        res.setName(name);
        res.setHallSpec(spec);
        res.setCapacity(capacity);
        res.setAvailable(available);
        res.setNote(note);
        resourceRepo.save(res);
    }

    private void r(String type, String name, String spec, boolean available, String note) {
        Resource res = new Resource();
        res.setType(type);
        res.setName(name);
        res.setHallSpec(spec);
        res.setAvailable(available);
        res.setNote(note);
        resourceRepo.save(res);
    }
}

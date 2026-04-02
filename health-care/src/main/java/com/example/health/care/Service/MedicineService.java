package com.example.health.care.Service;

import com.example.health.care.Entity.Medicine;
import com.example.health.care.Entity.MedicineOrder;
import com.example.health.care.Repository.MedicineOrderRepository;
import com.example.health.care.Repository.MedicineRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class MedicineService {

    @Autowired private MedicineRepository medicineRepo;
    @Autowired private MedicineOrderRepository orderRepo;

    @PostConstruct
    public void seedMedicines() {
        if (medicineRepo.count() > 0) return;

        Object[][] data = {
                {"Folic Acid 5mg","Folvite","Vitamins & Supplements","Pregnancy mein neural tube defects rokne ke liye zaroori.","1 tablet daily khane ke baad","🌿",45.0,60.0,100,true,"Neural tube development, Anemia prevention","Nausea (rare), Bloating"},
                {"Iron + Folic Acid","Autrin","Vitamins & Supplements","Iron aur folic acid combination — anemia rokta hai pregnancy mein.","1 capsule daily","💊",89.0,110.0,80,true,"Anemia, Iron deficiency, Hemoglobin badhana","Constipation, Dark stools"},
                {"Calcium + Vitamin D3","Shelcal 500","Bone Health","Baby ki haddiyon ke liye calcium aur Vitamin D3 combination.","1 tablet 2 baar daily","🦴",120.0,150.0,90,true,"Bone development, Calcium deficiency","Gas (rare)"},
                {"Vitamin D3 60000 IU","Calcirol","Vitamins & Supplements","Weekly Vitamin D3 supplement — doctor ki salah se lein.","1 capsule weekly","☀️",95.0,120.0,70,true,"Vitamin D deficiency, Immunity","Nausea if overdose"},
                {"Paracetamol 500mg","Dolo 650","Pain & Fever","Pregnancy mein safe pain reliever aur fever reducer.","1 tablet har 6 ghante (max 4/day)","🌡️",15.0,25.0,200,true,"Fever, Headache, Body pain","Liver damage (overdose mein)"},
                {"Omega-3 DHA 200mg","Natalcare DHA","Vitamins & Supplements","Baby ke brain aur eyes ke development ke liye DHA.","1 capsule daily","🐟",250.0,320.0,60,true,"Brain development, Eye development","Fish burps (mild)"},
                {"B6 Pyridoxine 10mg","Benadon","Nausea Relief","Morning sickness aur nausea ke liye Vitamin B6.","10-25mg 3 baar daily","🤢",45.0,60.0,100,true,"Morning sickness, Nausea, Vomiting","Nerve tingling (high dose)"},
                {"Antacid Gelusil","Gelusil MPS","Digestive","Pregnancy mein acidity aur heartburn ke liye safe antacid.","2 tablet khane ke 1 ghante baad","🔥",35.0,50.0,150,true,"Acidity, Heartburn, Indigestion","Constipation (prolonged use)"},
                {"Magnesium Glycinate","MagOx","Vitamins & Supplements","Leg cramps aur sleep ke liye magnesium supplement.","1 tablet raat ko","😴",180.0,220.0,50,true,"Leg cramps, Sleep, Anxiety","Loose stools (high dose)"},
                {"Prenatal Multivitamin","Materna","Vitamins & Supplements","Complete prenatal multivitamin — sab nutrients ek mein.","1 tablet daily with meal","✨",350.0,420.0,75,true,"Complete nutrition, Baby development","Nausea on empty stomach"},
                {"Lactulose Syrup","Duphalac","Digestive","Pregnancy mein constipation ke liye safe syrup.","15ml raat ko","🥄",145.0,180.0,60,true,"Constipation, Hard stools","Bloating, Gas"},
                {"Progesterone 200mg","Susten 200","Hormonal","Doctor ki salah se — miscarriage rokne ke liye.","Doctor ke anusar","💉",280.0,350.0,40,true,"Threatened miscarriage, Luteal support","Dizziness, Breast tenderness"},
                {"Zinc Supplement","Zincovit","Vitamins & Supplements","Immunity aur baby ke growth ke liye zinc supplement.","1 tablet daily","⚡",85.0,110.0,80,true,"Immunity, Growth, Wound healing","Nausea on empty stomach"},
                {"Cetirizine 10mg","Zyrtec","Allergy","2nd/3rd trimester mein allergies ke liye comparatively safe.","1 tablet raat ko","🤧",22.0,30.0,100,true,"Allergies, Itching, Hay fever","Drowsiness"},
                {"Pregnancy Test Kit","PregaNews","Diagnostics","Accurate pregnancy test — 99% accuracy.","Instructions ke anusar","🔬",60.0,80.0,200,true,"Pregnancy detection","N/A"},
        };

        for (Object[] d : data) {
            Medicine m = new Medicine();
            m.setName((String)d[0]);        m.setBrand((String)d[1]);
            m.setCategory((String)d[2]);    m.setDescription((String)d[3]);
            m.setDosage((String)d[4]);      m.setImageEmoji((String)d[5]);
            m.setPrice((Double)d[6]);       m.setMrp((Double)d[7]);
            m.setStock((Integer)d[8]);      m.setPregnancySafe((Boolean)d[9]);
            m.setUses((String)d[10]);       m.setSideEffects((String)d[11]);
            medicineRepo.save(m);
        }
        System.out.println("[MaatriCare] " + medicineRepo.count() + " medicines seeded successfully!");
    }

    public List<Medicine> getAll()                { return medicineRepo.findAll(); }
    public List<Medicine> search(String q)        { return medicineRepo.search(q); }
    public List<Medicine> getByCategory(String c) { return medicineRepo.findByCategory(c); }
    public List<Medicine> getPregnancySafe()      { return medicineRepo.findByPregnancySafe(true); }
    public Medicine getById(Long id) {
        return medicineRepo.findById(id).orElseThrow(() -> new RuntimeException("Medicine not found: " + id));
    }

    public MedicineOrder placeOrder(Map<String, Object> body) {
        MedicineOrder order = new MedicineOrder();
        order.setUserId(body.get("userId") != null ? Long.parseLong(body.get("userId").toString()) : null);
        order.setPatientName(body.get("patientName").toString());
        order.setAddress(body.get("address").toString());
        order.setPhone(body.get("phone").toString());
        order.setTotalAmount(Double.parseDouble(body.get("totalAmount").toString()));
        order.setPaymentMethod(body.get("paymentMethod").toString());
        order.setItemsJson(body.get("itemsJson").toString());
        order.setStatus("CONFIRMED");
        order.setCreatedAt(LocalDateTime.now());

        if ("COD".equals(order.getPaymentMethod())) {
            order.setPaymentStatus("PENDING");
            order.setTransactionId("COD-" + System.currentTimeMillis());
        } else {
            order.setPaymentStatus("PAID");
            order.setTransactionId("TXN-" + UUID.randomUUID().toString().substring(0,8).toUpperCase());
        }
        return orderRepo.save(order);
    }

    public List<MedicineOrder> getOrdersByUser(Long userId) {
        return orderRepo.findByUserIdOrderByCreatedAtDesc(userId);
    }
}
package ahqpck.maintenance.report.config;

import ahqpck.maintenance.report.dto.RoleDTO;
import ahqpck.maintenance.report.dto.UserDTO;
import ahqpck.maintenance.report.entity.Complaint;
import ahqpck.maintenance.report.entity.Part;
import ahqpck.maintenance.report.entity.Role;
import ahqpck.maintenance.report.entity.User;
import ahqpck.maintenance.report.entity.Complaint.Priority;
import ahqpck.maintenance.report.entity.Complaint.Category;
import ahqpck.maintenance.report.entity.Complaint.Status;
import ahqpck.maintenance.report.service.ComplaintService;
import ahqpck.maintenance.report.service.UserService;
import jakarta.transaction.Transactional;
import ahqpck.maintenance.report.repository.PartRepository;
import ahqpck.maintenance.report.repository.RoleRepository;
import ahqpck.maintenance.report.repository.UserRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @PostConstruct
    public void init() {
        try {
            initDefaultRoles();
            initDefaultUser();
            initBulkUsers(); // ← New bulk user creation
        } catch (Exception e) {
            log.error("Error during data initialization", e);
        }
    }

    private void initDefaultRoles() {
        log.info("Initializing default roles...");
        Arrays.stream(Role.Name.values()).forEach(roleName -> {
            Optional<Role> existingRole = roleRepository.findByName(roleName);
            if (existingRole.isPresent()) {
                log.debug("Role '{}' already exists.", roleName);
                return;
            }

            Role role = new Role();
            role.setName(roleName);
            roleRepository.save(role);
            log.info("Created role: {}", roleName);
        });
        log.info("Default roles initialization completed.");
    }

    private void initDefaultUser() {
        String email = "ggomugo@gmail.com";
        log.info("Checking if default user with email '{}' exists...", email);

        if (userRepository.findByEmail(email).isPresent()) {
            log.info("Default user with email '{}' already exists. Skipping creation.", email);
            return;
        }

        log.info("Creating default user with email '{}'", email);

        UserDTO userDTO = new UserDTO();
        userDTO.setName("Gema Nur");
        userDTO.setEmail(email);
        userDTO.setEmployeeId("0905");
        userDTO.setPassword("ahqpck123");
        userDTO.setStatus(User.Status.ACTIVE);
        userDTO.setCreatedAt(LocalDateTime.now());
        userDTO.setActivatedAt(LocalDateTime.now());

        Set<RoleDTO> roleDTOS = Arrays.stream(new Role.Name[] { Role.Name.SUPERADMIN, Role.Name.ADMIN })
                .map(name -> {
                    RoleDTO roleDTO = new RoleDTO();
                    roleDTO.setName(name);
                    return roleDTO;
                })
                .collect(Collectors.toSet());
        userDTO.setRoles(roleDTOS);

        try {
            userService.createUser(userDTO, null);
            log.info("Default user with email '{}' created successfully.", email);
        } catch (Exception e) {
            log.error("Failed to create default user with email '{}'", email, e);
        }
    }

    // ================== BULK USER CREATION ==================
    private void initBulkUsers() {
        log.info("Initializing bulk users...");

        // Define bulk user data: empNo, name, designation, nationality, joinDate, phone
        Object[][] userData = {
                { "0118", "Liu Gang", "Maintenance Manager", "Chinese", "27 Mar 2016", "055 572 3657" },
                { "0138", "Aqeel Ur Rehman", "Electrical Engineer", "Pakistani", "27 Nov 2016", "055 513 8417" },
                { "0942", "Waleed Ali", "Electrical Engineer", "Pakistani", "06 Apr 2025", "050 922 7196" },
                { "0910", "Khalid Ali Al-Osaimi", "Electrical Engineer", "Saudi", "16 Dec 2024", "059 510 8189" },
                { "0103", "Luo Dehui", "Electrical Technician", "Chinese", "25 Jan 2016", "055 849 3200" },
                { "0179", "Anthony Sacayan Solano", "Electrical Technician", "Filipino", "14 Nov 2017",
                        "053 881 2497" },
                { "0167", "Naseer Uddeen Faruqi", "Electrical Technician", "Indian", "12 May 2017", "059 272 3220" },
                { "0649", "Lal Dino", "Electrical Technician", "Pakistani", "10 Jul 2023", "056 482 1846" },
                { "0706", "Muhammad Yaseen", "Electrical Technician", "Pakistani", "09 Sep 2023", "053 061 6551" },
                { "0160", "Muhammad Rasheed", "Generator Operator", "Pakistani", "20 Mar 2017", "058 195 8942" },
                { "0675", "Mumtaz Ali", "Generator Operator", "Pakistani", "06 Aug 2023", "057 540 4568" },
                { "0707", "Saeed Ahmed", "Generator Operator", "Pakistani", "09 Sep 2023", "053 675 2148" },
                { "0866", "Ali Murtaza Jameel", "Generator Operator", "Indian", "23 Oct 2024", "050 161 0508" },
                { "0296", "Muhammad Sohail", "AC Technician", "Pakistani", "25 Jul 2019", "059 072 4042" },
                { "0638", "Muhammad Raheel", "AC Technician", "Pakistani", "17 Jun 2023", "053 756 6912" },
                { "0881", "Hussain Ali Al Nasser", "AC Technician", "Saudi", "06 Oct 2024", "054 848 4448" },
                { "0363", "Muhammad Saleem", "Motor Rewinder", "Pakistani", "15 Feb 2020", "059 027 4216" },
                { "0054", "Noah Al Muallem", "Electrical Helper", "Saudi", "03 Sep 2015", "054 260 0465" },
                { "0149", "Abbas Mekki Al Sheikh", "Electrical Helper", "Saudi", "25 Dec 2016", "056 917 7020" },
                { "0550", "Fahad Rehman", "Mechanical Engineer", "Pakistani", "11 Jan 2023", "050 193 0247" },
                { "0773", "Rahool Rathore", "Mechanical Engineer", "Pakistani", "21 Dec 2023", "053 808 6406" },
                { "0956", "Vinay Kumar Satti", "Mechanical Engineer", "Indian", "22 May 2025", "050 251 2405" },
                { "0065", "Xiao Zhibing", "Mechanical Technician", "Chinese", "05 Dec 2015", "055 518 0783" },
                { "0255", "Hariom Prakash", "Mechanical Technician", "Indian", "22 Mar 2019", "059 945 0271" },
                { "0487", "Rajab Ali", "Mechanical Technician", "Pakistani", "07 Jul 2022", "056 187 0361" },
                { "0968", "Muhammad Ayoub Sahoo", "Mechanical Technician", "Pakistani", "03 Aug 2025", "059 598 1149" },
                { "0632", "Naseer Abbasi", "Mechanical Technician", "Pakistani", "11 Jun 2023", "059 753 5772" },
                { "0816", "Pankaj Madheshiya", "Mechanical Technician", "Indian", "18 Mar 2024", "059 530 4407" },
                { "0840", "Marlou Paurom Ranara", "Mechanical Technician", "Filipino", "14 Jul 2024", "056 117 9442" },
                { "0865", "Achhelal Kushwaha", "Mechanical Technician", "Indian", "24 Sep 2024", "055 832 6037" },
                { "0053", "Alex Alcoriza Tiongco", "Mechanic/Welder", "Filipino", "18 Aug 2015", "055 154 5799" },
                { "0451", "Sanjay Dhrupdev Pathak", "Mechanic/Welder", "Indian", "04 Jul 2022", "051 065 5813" },
                { "0900", "Sartajuddin Faruki", "Mechanic/Welder", "Indian", "06 Nov 2024", "056 897 0828" },
                { "0330", "Ali Nassir Al Obeidan", "Mechanic", "Saudi", "12 Oct 2019", "054 456 6910" },
                { "0695", "Hassan Abdullah Al Nasser", "Mechanic", "Saudi", "23 Aug 2023", "053 747 6613" },
                { "0823", "Husain Al Ashwan", "Mechanic", "Saudi", "", "057 023 5423" },
                { "0906", "Ali Al Taha", "Mechanic", "Saudi", "01 Dec 2024", "056 720 5289" },
                { "0978", "Hussain Ali Al Mohsin", "Mechanic", "Saudi", "24 Aug 2025", "053 054 5255" }
        };

        // Get VIEWER role
        Role viewerRole = roleRepository.findByName(Role.Name.VIEWER)
                .orElseGet(() -> {
                    log.warn("VIEWER role not found. Creating it.");
                    Role role = new Role();
                    role.setName(Role.Name.VIEWER);
                    return roleRepository.save(role);
                });

        for (Object[] userRow : userData) {
            String empNo = (String) userRow[0];
            String name = (String) userRow[1];
            String designation = (String) userRow[2];
            String nationality = (String) userRow[3];
            String joinDateStr = (String) userRow[4];
            String phone = (String) userRow[5];

            try {
                // Skip if join date is empty
                LocalDate joinDate = null;
                if (joinDateStr != null && !joinDateStr.trim().isEmpty()) {
                    joinDate = LocalDate.parse(joinDateStr, DateTimeFormatter.ofPattern("dd MMM yyyy"));
                }

                // Generate email: firstname.lastname@company.com
                String email = name.trim()
                        .toLowerCase()
                        .replaceAll("\\s+", ".") // Replace spaces with dots
                        .replaceAll("[^a-z.]", "") + "@email.com";

                // Ensure email is unique (fallback)
                String originalEmail = email;
                int suffix = 1;
                while (userRepository.findByEmail(email).isPresent()) {
                    email = originalEmail.replace("@", suffix + "@");
                    suffix++;
                }

                if (userRepository.findByEmail(email).isPresent()) {
                    log.debug("User with email '{}' already exists. Skipping.", email);
                    continue;
                }

                UserDTO userDTO = new UserDTO();
                userDTO.setName(name);
                userDTO.setEmail(email);
                userDTO.setEmployeeId(empNo);
                userDTO.setPassword("12345678"); // Default password
                userDTO.setDesignation(designation);
                userDTO.setNationality(nationality);
                userDTO.setJoinDate(joinDate);
                userDTO.setPhoneNumber(phone);
                userDTO.setStatus(User.Status.ACTIVE);
                userDTO.setCreatedAt(LocalDateTime.now());

                // Assign role
                RoleDTO roleDTO = new RoleDTO();
                roleDTO.setName(Role.Name.VIEWER);
                userDTO.setRoles(Set.of(roleDTO));

                userService.createUser(userDTO, null);
                log.info("Created user: {} ({}), Email: {}", name, empNo, email);

            } catch (Exception e) {
                log.error("Failed to create user: {} (Emp: {})", name, empNo, e);
            }
        }

        log.info("Bulk user initialization completed.");
    }
}

// private static final Logger log =
// LoggerFactory.getLogger(DataInitializer.class);

// private final PartRepository partRepository;
// private final ComplaintService complaintService;

// public DataInitializer(PartRepository partRepository, ComplaintService
// complaintService) {
// this.partRepository = partRepository;
// this.complaintService = complaintService;
// }

// @Override
// public void run(String... args) throws Exception {
// log.info("Starting test data seeding with multiple CLOSED complaints...");

// // === Seed Common Parts ===
// Part motor = Part.builder()
// .code("MTR-2001")
// .name("AC Motor 1.5kW")
// .category("Mechanical")
// .supplier("MotoTech Inc.")
// .stockQuantity(15999999)
// .build();

// Part bearing = Part.builder()
// .code("BRG-8890")
// .name("Ball Bearing 6205")
// .category("Mechanical")
// .supplier("Precision Bearings Co.")
// .stockQuantity(60999999)
// .build();

// Part sensor = Part.builder()
// .code("SNS-3030")
// .name("Proximity Sensor NPN")
// .category("Electrical")
// .supplier("AutoSense Ltd.")
// .stockQuantity(259999)
// .build();

// Part fuse = Part.builder()
// .code("FUS-4040")
// .name("Fuse 10A Fast-Blow")
// .category("Electrical")
// .supplier("CircuitSafe")
// .stockQuantity(1209999)
// .build();

// // Save parts only if not already present
// Arrays.asList(motor, bearing, sensor, fuse).forEach(part -> {
// if (!partRepository.existsByCodeIgnoreCase(part.getCode())) {
// partRepository.save(part);
// log.info("Saved part: {} (Code: {})", part.getName(), part.getCode());
// } else {
// log.info("Part already exists: {}", part.getCode());
// }
// });

// // === CLOSED Complaint 1: Motor Replacement ===
// log.info("Creating CLOSED complaint #1: Motor Failure");
// Complaint complaint1 = Complaint.builder()
// .area("Production Line A")
// .machine("Conveyor-5")
// .reporter("John Doe")
// .subject("Motor Burned Out")
// .description("Main drive motor failed due to overload.")
// .priority(Priority.HIGH)
// .category(Category.MECHANICAL)
// .assignee("Alice")
// .build();

// complaint1 = complaintService.createComplaint(complaint1);
// complaintService.addPartToComplaint(complaint1.getId(), "MTR-2001", 1);
// complaintService.addPartToComplaint(complaint1.getId(), "BRG-8890", 2);
// complaintService.updateStatus(complaint1.getId(), Status.CLOSED);
// log.info("COMPLAINT CLOSED #1: Motor replaced 1x MTR-2001, 2x BRG-8890
// deducted");

// // === CLOSED Complaint 2: Sensor Fault ===
// log.info("Creating CLOSED complaint #2: Sensor Malfunction");
// Complaint complaint2 = Complaint.builder()
// .area("Automation Zone")
// .machine("Robot-Arm-3")
// .reporter("Tech-Supervisor")
// .subject("Position Sensor Not Responding")
// .description("Robot fails to detect end position. Sensor replaced.")
// .priority(Priority.MEDIUM)
// .category(Category.ELECTRICAL)
// .assignee("David")
// .build();

// complaint2 = complaintService.createComplaint(complaint2);
// complaintService.addPartToComplaint(complaint2.getId(), "SNS-3030", 1);
// complaintService.updateStatus(complaint2.getId(), Status.CLOSED);
// log.info("COMPLAINT CLOSED #2: Sensor replaced 1x SNS-3030 deducted");

// // === CLOSED Complaint 3: Electrical Fuse Blown ===
// log.info("Creating CLOSED complaint #3: Fuse Blown in Panel");
// Complaint complaint3 = Complaint.builder()
// .area("Electrical Room")
// .machine("Main Control Panel-2")
// .reporter("Engineer-Y")
// .subject("Fuse Tripped Repeatedly")
// .description("Fuse blew due to short. Replaced and issue resolved after
// wiring check.")
// .priority(Priority.HIGH)
// .category(Category.ELECTRICAL)
// .assignee("Grace")
// .build();

// complaint3 = complaintService.createComplaint(complaint3);
// complaintService.addPartToComplaint(complaint3.getId(), "FUS-4040", 3); // 3
// fuses used
// complaintService.updateStatus(complaint3.getId(), Status.CLOSED);
// log.info("COMPLAINT CLOSED #3: Fuse replaced 3x FUS-4040 deducted");

// // Optional: Reopen one to test restock
// Thread.sleep(1000);
// complaintService.reopenComplaint(complaint2.getId());
// log.info("COMPLAINT REOPENED: #{} 1x SNS-3030 restocked",
// complaint2.getId());

// // === Final Summary ===
// log.info("Test data seeding completed with multiple CLOSED complaints.");
// log.info("Inventory deductions confirmed for:");
// log.info("Motor (MTR-2001): -1");
// log.info("Bearing (BRG-8890): -2");
// log.info("Sensor (SNS-3030): -1 / +1 (after reopen)");
// log.info("Fuse (FUS-4040): -3");
// }
// }
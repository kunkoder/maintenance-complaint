package ahqpck.maintenance.report.config;

import ahqpck.maintenance.report.entity.Complaint;
import ahqpck.maintenance.report.entity.Part;
import ahqpck.maintenance.report.entity.Role;
import ahqpck.maintenance.report.entity.User;
import ahqpck.maintenance.report.entity.Complaint.Priority;
import ahqpck.maintenance.report.entity.Complaint.Category;
import ahqpck.maintenance.report.entity.Complaint.Status;
import ahqpck.maintenance.report.service.ComplaintService;
import ahqpck.maintenance.report.service.UserService;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import ahqpck.maintenance.report.repository.PartRepository;
import ahqpck.maintenance.report.repository.RoleRepository;
import ahqpck.maintenance.report.repository.UserRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;

@Component
// public class DataInitializer implements CommandLineRunner {
public class DataInitializer {
    @Autowired
    private RoleRepository roleRepository;

    @PostConstruct
    public void initDefaultRoles() {
        Arrays.stream(Role.Name.values()).forEach(name -> {
            roleRepository.findByName(name).orElseGet(() -> {
                Role role = new Role();
                role.setName(name);
                return roleRepository.save(role);
            });
        });
    }

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @PostConstruct
    public void init() {
        initDefaultRoles();
        initDefaultUser();
    }

    // @Transactional
    // public void initDefaultRoles() {
    //     Arrays.stream(Role.Name.values()).forEach(name -> {
    //         roleRepository.findByName(name).orElseGet(() -> {
    //             Role role = new Role();
    //             role.setName(name);
    //             return roleRepository.save(role);
    //         });
    //     });
    // }

    @Transactional
    public void initDefaultUser() {
        String email = "ggomugo@gmail.com";
        Optional<User> existingUser = userRepository.findByEmail(email);

        if (existingUser.isPresent()) {
            // Optional: log that user already exists
            return;
        }

        // Find required roles
        Role superAdminRole = roleRepository.findByName(Role.Name.SUPERADMIN)
                .orElseThrow(() -> new IllegalStateException("SUPERADMIN role not found. Run role init first."));
        Role adminRole = roleRepository.findByName(Role.Name.ADMIN)
                .orElseThrow(() -> new IllegalStateException("ADMIN role not found. Run role init first."));

        // Create user entity
        User user = new User();
        user.setName("Gema Nur");
        user.setEmail(email);
        user.setEmployeeId("0905");
        user.setStatus(User.Status.ACTIVE);
        user.setCreatedAt(LocalDateTime.now());
        user.setActivatedAt(LocalDateTime.now());

        // Assign roles
        user.getRoles().add(superAdminRole);
        user.getRoles().add(adminRole);

        // Save directly (or use service if you prefer validation)
        userRepository.save(user);
    }

}




//     private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

//     private final PartRepository partRepository;
//     private final ComplaintService complaintService;

//     public DataInitializer(PartRepository partRepository, ComplaintService complaintService) {
//         this.partRepository = partRepository;
//         this.complaintService = complaintService;
//     }

//     @Override
//     public void run(String... args) throws Exception {
//         log.info("Starting test data seeding with multiple CLOSED complaints...");

//         // === Seed Common Parts ===
//         Part motor = Part.builder()
//             .code("MTR-2001")
//             .name("AC Motor 1.5kW")
//             .category("Mechanical")
//             .supplier("MotoTech Inc.")
//             .stockQuantity(15999999)
//             .build();

//         Part bearing = Part.builder()
//             .code("BRG-8890")
//             .name("Ball Bearing 6205")
//             .category("Mechanical")
//             .supplier("Precision Bearings Co.")
//             .stockQuantity(60999999)
//             .build();

//         Part sensor = Part.builder()
//             .code("SNS-3030")
//             .name("Proximity Sensor NPN")
//             .category("Electrical")
//             .supplier("AutoSense Ltd.")
//             .stockQuantity(259999)
//             .build();

//         Part fuse = Part.builder()
//             .code("FUS-4040")
//             .name("Fuse 10A Fast-Blow")
//             .category("Electrical")
//             .supplier("CircuitSafe")
//             .stockQuantity(1209999)
//             .build();

//         // Save parts only if not already present
//         Arrays.asList(motor, bearing, sensor, fuse).forEach(part -> {
//             if (!partRepository.existsByCodeIgnoreCase(part.getCode())) {
//                 partRepository.save(part);
//                 log.info("Saved part: {} (Code: {})", part.getName(), part.getCode());
//             } else {
//                 log.info("Part already exists: {}", part.getCode());
//             }
//         });

//         // === CLOSED Complaint 1: Motor Replacement ===
//         log.info("Creating CLOSED complaint #1: Motor Failure");
//         Complaint complaint1 = Complaint.builder()
//             .area("Production Line A")
//             .machine("Conveyor-5")
//             .reporter("John Doe")
//             .subject("Motor Burned Out")
//             .description("Main drive motor failed due to overload.")
//             .priority(Priority.HIGH)
//             .category(Category.MECHANICAL)
//             .assignee("Alice")
//             .build();

//         complaint1 = complaintService.createComplaint(complaint1);
//         complaintService.addPartToComplaint(complaint1.getId(), "MTR-2001", 1);
//         complaintService.addPartToComplaint(complaint1.getId(), "BRG-8890", 2);
//         complaintService.updateStatus(complaint1.getId(), Status.CLOSED);
//         log.info("COMPLAINT CLOSED #1: Motor replaced 1x MTR-2001, 2x BRG-8890 deducted");

//         // === CLOSED Complaint 2: Sensor Fault ===
//         log.info("Creating CLOSED complaint #2: Sensor Malfunction");
//         Complaint complaint2 = Complaint.builder()
//             .area("Automation Zone")
//             .machine("Robot-Arm-3")
//             .reporter("Tech-Supervisor")
//             .subject("Position Sensor Not Responding")
//             .description("Robot fails to detect end position. Sensor replaced.")
//             .priority(Priority.MEDIUM)
//             .category(Category.ELECTRICAL)
//             .assignee("David")
//             .build();

//         complaint2 = complaintService.createComplaint(complaint2);
//         complaintService.addPartToComplaint(complaint2.getId(), "SNS-3030", 1);
//         complaintService.updateStatus(complaint2.getId(), Status.CLOSED);
//         log.info("COMPLAINT CLOSED #2: Sensor replaced 1x SNS-3030 deducted");

//         // === CLOSED Complaint 3: Electrical Fuse Blown ===
//         log.info("Creating CLOSED complaint #3: Fuse Blown in Panel");
//         Complaint complaint3 = Complaint.builder()
//             .area("Electrical Room")
//             .machine("Main Control Panel-2")
//             .reporter("Engineer-Y")
//             .subject("Fuse Tripped Repeatedly")
//             .description("Fuse blew due to short. Replaced and issue resolved after wiring check.")
//             .priority(Priority.HIGH)
//             .category(Category.ELECTRICAL)
//             .assignee("Grace")
//             .build();

//         complaint3 = complaintService.createComplaint(complaint3);
//         complaintService.addPartToComplaint(complaint3.getId(), "FUS-4040", 3); // 3 fuses used
//         complaintService.updateStatus(complaint3.getId(), Status.CLOSED);
//         log.info("COMPLAINT CLOSED #3: Fuse replaced 3x FUS-4040 deducted");

//         // Optional: Reopen one to test restock
//         Thread.sleep(1000);
//         complaintService.reopenComplaint(complaint2.getId());
//         log.info("COMPLAINT REOPENED: #{} 1x SNS-3030 restocked", complaint2.getId());

//         // === Final Summary ===
//         log.info("Test data seeding completed with multiple CLOSED complaints.");
//         log.info("Inventory deductions confirmed for:");
//         log.info("Motor (MTR-2001): -1");
//         log.info("Bearing (BRG-8890): -2");
//         log.info("Sensor (SNS-3030): -1 / +1 (after reopen)");
//         log.info("Fuse (FUS-4040): -3");
//     }
// }
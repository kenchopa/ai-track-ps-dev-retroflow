package com.stackcraft.retroflow;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaType;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;
import java.util.stream.Stream;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_USE_FIELD_INJECTION;

/**
 * The structure decided in Block 1 and enforced since Block 2: if one of
 * these fails, the code drifted, not the test.
 */
@AnalyzeClasses(packages = "com.stackcraft.retroflow", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    /** Controllers go through the service layer, where the business rules are. */
    @ArchTest
    static final ArchRule controllersDoNotUseRepositories = noClasses()
            .that().resideInAPackage("..controller..")
            .should().dependOnClassesThat().resideInAPackage("..repository..")
            .because("business rules are enforced in the service layer (brief, decisions.md G2)");

    /** No entity in any request or response, not even inside a List (decisions.md G5). */
    @ArchTest
    static final ArchRule controllersDoNotExposeEntities = methods()
            .that().areDeclaredInClassesThat().areAnnotatedWith(RestController.class)
            .and().arePublic()
            .should(notReturnOrAcceptEntities());

    /** The service layer must not know about HTTP: no controllers, no request/response records. */
    @ArchTest
    static final ArchRule servicesDoNotDependOnWebLayer = noClasses()
            .that().resideInAPackage("..service..")
            .should().dependOnClassesThat().resideInAnyPackage("..controller..", "..dto..");

    /** Constructor injection only (StackCraft standard 1.4). */
    @ArchTest
    static final ArchRule noFieldInjection = NO_CLASSES_SHOULD_USE_FIELD_INJECTION;

    private static ArchCondition<JavaMethod> notReturnOrAcceptEntities() {
        return new ArchCondition<>("not return or accept entities") {
            @Override
            public void check(JavaMethod method, ConditionEvents events) {
                Stream.concat(Stream.of(method.getReturnType()), method.getParameterTypes().stream())
                        .map(JavaType::getAllInvolvedRawTypes)
                        .flatMap(Set::stream)
                        .filter(type -> type.getPackageName().endsWith(".entity"))
                        .map(JavaClass::getSimpleName)
                        .forEach(entity -> events.add(SimpleConditionEvent.violated(method,
                                method.getFullName() + " exposes entity " + entity)));
            }
        };
    }

}

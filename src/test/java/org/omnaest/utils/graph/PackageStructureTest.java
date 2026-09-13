package org.omnaest.utils.graph;

import org.junit.jupiter.api.Test;
import org.omnaest.utils.style.StyleProfile;
import org.omnaest.utils.style.sourcetext.SourceGuard;

/**
 * Mechanically enforces this workspace's Java package structure guideline (see
 * {@code .claude/guidelines/java-package-structure.md}) against {@code CommonsGraph} via
 * {@code CommonsStyleSupport}'s LIBRARY profile (plan-188), reformulated onto the relative {@code internal/} model
 * (plan-193).
 * <p>
 * One {@code @Test} method per check, so a project opts out of one check by deleting one line. This class is
 * test-only structural-assertion infrastructure with no main-source counterpart, so it is exempt from the
 * test-mirror rule (P13) and legitimately sits at the context root.
 * <p>
 * Only the shipped {@code SUBTREE} readings are asserted here. The strict {@code DIRECT_PARENT} reading is a
 * plan-193 Cliff 3 measurement mode, not a permanent check, and is exercised separately by a temporary probe.
 * <p>
 * plan-216: this class now runs the full enforced check surface (14 {@code ArchRule} factories plus 2
 * {@code SourceGuard} checks), bringing it up from the smaller subset it adopted with. Deliberately excluded, both
 * on documented measurement-only grounds rather than by omission: {@code StyleProfile}'s
 * {@code internalPackagesAreAccessedOnlyFromTheirDirectParentPackage()} and {@code SourceGuard}'s
 * {@code noInternalReferencesFromOutsideTheirDirectParentPackage()} - the strict {@code DIRECT_PARENT} readings
 * named above.
 */
class PackageStructureTest
{

    private static final StyleProfile PROFILE = StyleProfile.library("org.omnaest.utils.graph");

    @Test
    void singleEntryPointAtContextRoot()
    {
        PROFILE.singleEntryPointAtContextRoot()
               .check(PROFILE.mainClasses());
    }

    @Test
    void entryPointIsInterfaceOrUtilsFactory()
    {
        PROFILE.entryPointIsInterfaceOrUtilsFactory()
               .check(PROFILE.mainClasses());
    }

    @Test
    void noHorizontalLayerPackages()
    {
        PROFILE.noHorizontalLayerPackages()
               .check(PROFILE.mainClasses());
    }

    @Test
    void internalPackagesAreAccessedOnlyFromWithinTheirOwnSubtree()
    {
        PROFILE.internalPackagesAreAccessedOnlyFromWithinTheirOwnSubtree()
               .check(PROFILE.mainClasses());
    }

    @Test
    void repositoryTypesLiveInInternalRepository()
    {
        PROFILE.repositoryTypesLiveInInternalRepository()
               .check(PROFILE.mainClasses());
    }

    @Test
    void noDtoTypesOutsideInternal()
    {
        PROFILE.noDtoTypesOutsideInternal()
               .check(PROFILE.mainClasses());
    }

    @Test
    void internalSubPackagesAreRoleNamed()
    {
        PROFILE.internalSubPackagesAreRoleNamed()
               .check(PROFILE.mainClasses());
    }

    @Test
    void noInternalTypeOnAPublicApiSurface()
    {
        PROFILE.noInternalTypeOnAPublicApiSurface()
               .check(PROFILE.mainClasses());
    }

    @Test
    void boundedContextsAreDiscovered()
    {
        PROFILE.boundedContextsAreDiscovered()
               .check(PROFILE.mainClasses());
    }

    @Test
    void noContextDependsOnAnAdapter()
    {
        PROFILE.noContextDependsOnAnAdapter()
               .check(PROFILE.mainClasses());
    }

    @Test
    void adapterWireTypesLiveInTheirChannelDomain()
    {
        PROFILE.adapterWireTypesLiveInTheirChannelDomain()
               .check(PROFILE.mainClasses());
    }

    @Test
    void sharedTypesAreUsedByAtLeastTwoContexts()
    {
        PROFILE.sharedTypesAreUsedByAtLeastTwoContexts()
               .check(PROFILE.mainClasses());
    }

    @Test
    void utilsPackagesDoNotReachIntoDomain()
    {
        PROFILE.utilsPackagesDoNotReachIntoDomain()
               .check(PROFILE.mainClasses());
    }

    @Test
    void utilsPackagesDoNotDuplicateCommonsTypes()
    {
        PROFILE.utilsPackagesDoNotDuplicateCommonsTypes()
               .check(PROFILE.mainClasses());
    }

    @Test
    void noInternalReferencesFromOutsideTheirOwnSubtree()
    {
        SourceGuard.of()
                   .noInternalReferencesFromOutsideTheirOwnSubtree()
                   .verify();
    }

    @Test
    void testsMirrorTheirSubjectPackage()
    {
        SourceGuard.of()
                   .testsMirrorTheirSubjectPackage()
                   .verify();
    }

}

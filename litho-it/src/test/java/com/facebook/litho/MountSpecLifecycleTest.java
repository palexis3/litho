/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.litho;

import static com.facebook.litho.LifecycleStep.getSteps;
import static com.facebook.litho.SizeSpec.EXACTLY;
import static com.facebook.litho.SizeSpec.UNSPECIFIED;
import static com.facebook.litho.SizeSpec.makeSizeSpec;
import static org.assertj.core.api.Java6Assertions.assertThat;

import android.content.ContextWrapper;
import android.os.Looper;
import android.view.View;
import androidx.collection.LongSparseArray;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.testing.LithoStatsRule;
import com.facebook.litho.testing.LithoViewRule;
import com.facebook.litho.testing.testrunner.LithoTestRunner;
import com.facebook.litho.widget.MountSpecLifecycleTester;
import com.facebook.litho.widget.MountSpecLifecycleTesterSpec;
import com.facebook.litho.widget.PreallocatedMountSpecLifecycleTester;
import com.facebook.litho.widget.ShouldUseGlobalPoolFalseMountSpecLifecycleTester;
import com.facebook.litho.widget.ShouldUseGlobalPoolTrueMountSpecLifecycleTester;
import com.facebook.rendercore.MountItem;
import java.util.ArrayList;
import java.util.List;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.shadows.ShadowLooper;

@RunWith(LithoTestRunner.class)
public class MountSpecLifecycleTest {

  public final @Rule LithoViewRule mLithoViewRule = new LithoViewRule();
  public final @Rule LithoStatsRule mLithoStatsRule = new LithoStatsRule();

  @After
  public void after() {
    MountSpecLifecycleTesterSpec.StaticContainer.sLastCreatedView = null;
  }

  @Test
  public void lifecycle_onSetComponentWithoutLayout_shouldNotCallLifecycleMethods() {
    final List<LifecycleStep.StepInfo> info = new ArrayList<>();
    final Component component =
        MountSpecLifecycleTester.create(mLithoViewRule.getContext()).steps(info).build();
    mLithoViewRule.setRoot(component);

    assertThat(getSteps(info)).describedAs("No lifecycle methods should be called").isEmpty();
  }

  @Test
  public void lifecycle_onLayout_shouldCallLifecycleMethods() {
    final List<LifecycleStep.StepInfo> info = new ArrayList<>();
    final Component component =
        MountSpecLifecycleTester.create(mLithoViewRule.getContext()).steps(info).build();
    mLithoViewRule.setRoot(component);

    mLithoViewRule.attachToWindow().measure().layout();

    assertThat(getSteps(info))
        .describedAs("Should call the lifecycle methods in expected order")
        .containsExactly(
            LifecycleStep.ON_CREATE_TREE_PROP,
            LifecycleStep.ON_CALCULATE_CACHED_VALUE,
            LifecycleStep.ON_PREPARE,
            LifecycleStep.ON_MEASURE,
            LifecycleStep.ON_BOUNDS_DEFINED,
            LifecycleStep.ON_ATTACHED,
            LifecycleStep.ON_CREATE_MOUNT_CONTENT,
            LifecycleStep.ON_MOUNT,
            LifecycleStep.ON_BIND);

    assertThat(ComponentsPools.getMountContentPools().size())
        .describedAs("Should contain only 1 content pool")
        .isEqualTo(1);
    assertThat(ComponentsPools.getMountContentPools().get(0).getName())
        .describedAs("Should contain content pool from MountSpecLifecycleTester")
        .isEqualTo("MountSpecLifecycleTester");
  }

  @Test
  public void lifecycle_onLayoutWithExactSize_shouldCallLifecycleMethodsExceptMeasure() {
    final List<LifecycleStep.StepInfo> info = new ArrayList<>();
    final Component component =
        MountSpecLifecycleTester.create(mLithoViewRule.getContext()).steps(info).build();
    mLithoViewRule.setRoot(component);

    mLithoViewRule.setSizePx(600, 800).attachToWindow().measure().layout();

    assertThat(getSteps(info))
        .describedAs("Should call the lifecycle methods in expected order")
        .containsExactly(
            LifecycleStep.ON_CREATE_TREE_PROP,
            LifecycleStep.ON_CALCULATE_CACHED_VALUE,
            LifecycleStep.ON_PREPARE,
            LifecycleStep.ON_BOUNDS_DEFINED,
            LifecycleStep.ON_ATTACHED,
            LifecycleStep.ON_CREATE_MOUNT_CONTENT,
            LifecycleStep.ON_MOUNT,
            LifecycleStep.ON_BIND);
  }

  @Test
  public void lifecycle_onDetach_shouldCallLifecycleMethods() {
    final List<LifecycleStep.StepInfo> info = new ArrayList<>();
    final Component component =
        MountSpecLifecycleTester.create(mLithoViewRule.getContext()).steps(info).build();
    mLithoViewRule.setRoot(component);

    mLithoViewRule.attachToWindow().measure().layout();

    info.clear();

    mLithoViewRule.detachFromWindow();

    assertThat(getSteps(info))
        .describedAs("Should only call")
        .containsExactly(LifecycleStep.ON_UNBIND);
  }

  @Test
  public void lifecycle_onReAttach_shouldCallLifecycleMethods() {
    final List<LifecycleStep.StepInfo> info = new ArrayList<>();
    final Component component =
        MountSpecLifecycleTester.create(mLithoViewRule.getContext()).steps(info).build();
    mLithoViewRule.setRoot(component);

    mLithoViewRule.attachToWindow().measure().layout().detachFromWindow();

    info.clear();

    mLithoViewRule.attachToWindow().measure().layout();

    assertThat(getSteps(info))
        .describedAs("Should only call")
        .containsExactly(LifecycleStep.ON_BIND);
  }

  @Test
  public void lifecycle_onRemeasureWithSameSpecs_shouldNotCallLifecycleMethods() {
    final List<LifecycleStep.StepInfo> info = new ArrayList<>();
    final Component component =
        MountSpecLifecycleTester.create(mLithoViewRule.getContext()).steps(info).build();
    mLithoViewRule.setRoot(component);

    mLithoViewRule.attachToWindow().measure().layout();

    info.clear();

    mLithoViewRule.measure();

    assertThat(getSteps(info)).describedAs("No lifecycle methods should be called").isEmpty();
  }

  @Test
  public void lifecycle_onRemeasureWithDifferentSpecs_shouldCallLifecycleMethods() {
    final List<LifecycleStep.StepInfo> info = new ArrayList<>();
    final Component component =
        MountSpecLifecycleTester.create(mLithoViewRule.getContext()).steps(info).build();
    mLithoViewRule.setRoot(component);

    mLithoViewRule.attachToWindow().measure().layout();

    info.clear();

    mLithoViewRule
        .setSizeSpecs(makeSizeSpec(800, EXACTLY), makeSizeSpec(600, UNSPECIFIED))
        .measure();

    assertThat(getSteps(info))
        .describedAs("Should call the lifecycle methods in expected order")
        .containsExactly(
            LifecycleStep.ON_CREATE_TREE_PROP,
            LifecycleStep.ON_CALCULATE_CACHED_VALUE,
            LifecycleStep.ON_PREPARE,
            LifecycleStep.ON_MEASURE,
            LifecycleStep.ON_BOUNDS_DEFINED);
  }

  @Test
  public void lifecycle_onRemeasureWithExactSize_shouldNotCallLifecycleMethods() {
    final List<LifecycleStep.StepInfo> info = new ArrayList<>();
    final Component component =
        MountSpecLifecycleTester.create(mLithoViewRule.getContext()).steps(info).build();
    mLithoViewRule.setRoot(component);

    mLithoViewRule.attachToWindow().measure().layout();

    info.clear();

    mLithoViewRule.setSizePx(800, 600).measure();

    assertThat(getSteps(info))
        .describedAs(
            "No lifecycle methods should be called because EXACT measures should skip layout calculation.")
        .isEmpty();
  }

  @Test
  public void lifecycle_onReLayoutAfterMeasureWithExactSize_shouldCallLifecycleMethods() {
    final List<LifecycleStep.StepInfo> info = new ArrayList<>();
    final Component component =
        MountSpecLifecycleTester.create(mLithoViewRule.getContext()).steps(info).build();
    mLithoViewRule.setRoot(component);

    mLithoViewRule.attachToWindow().measure().layout();

    info.clear();

    mLithoViewRule.setSizePx(800, 600).measure().layout();

    assertThat(getSteps(info))
        .describedAs("Should call the lifecycle methods in expected order")
        .containsExactly(
            LifecycleStep.ON_CREATE_TREE_PROP,
            LifecycleStep.ON_CALCULATE_CACHED_VALUE,
            LifecycleStep.ON_PREPARE,
            LifecycleStep.ON_BOUNDS_DEFINED,
            LifecycleStep.ON_UNBIND,
            LifecycleStep.ON_UNMOUNT,
            LifecycleStep.ON_MOUNT,
            LifecycleStep.ON_BIND);
  }

  @Test
  public void lifecycle_onReLayoutAfterMeasureWithExactSizeAsNonRoot_shouldCallLifecycleMethods() {
    final List<LifecycleStep.StepInfo> info = new ArrayList<>();
    final Component component =
        MountSpecLifecycleTester.create(mLithoViewRule.getContext()).steps(info).build();
    mLithoViewRule.setRoot(Column.create(mLithoViewRule.getContext()).child(component).build());

    mLithoViewRule.attachToWindow().measure().layout();

    info.clear();

    mLithoViewRule.setSizePx(800, 600).measure().layout();

    assertThat(getSteps(info))
        .describedAs("Should call the lifecycle methods in expected order")
        .containsExactly(
            LifecycleStep.ON_CREATE_TREE_PROP,
            LifecycleStep.ON_CALCULATE_CACHED_VALUE,
            LifecycleStep.ON_PREPARE,
            LifecycleStep.ON_MEASURE,
            LifecycleStep.ON_BOUNDS_DEFINED,
            LifecycleStep.ON_UNBIND,
            LifecycleStep.ON_UNMOUNT,
            LifecycleStep.ON_MOUNT,
            LifecycleStep.ON_BIND);
  }

  @Test
  public void lifecycle_onSetShallowCopy_shouldNotCallLifecycleMethods() {
    final List<LifecycleStep.StepInfo> info = new ArrayList<>();
    final Component component =
        MountSpecLifecycleTester.create(mLithoViewRule.getContext()).steps(info).build();
    mLithoViewRule.setRoot(component);

    mLithoViewRule.attachToWindow().measure().layout();

    info.clear();

    mLithoViewRule.setRoot(component.makeShallowCopy());

    assertThat(getSteps(info)).describedAs("No lifecycle methods should be called").isEmpty();
  }

  @Test
  public void lifecycle_onSetSemanticallySimilarComponent_shouldCallLifecycleMethods() {
    final List<LifecycleStep.StepInfo> info = new ArrayList<>();
    final Component component =
        MountSpecLifecycleTester.create(mLithoViewRule.getContext()).steps(info).build();
    mLithoViewRule.setRoot(component);

    mLithoViewRule.attachToWindow().measure().layout();

    info.clear();

    List<LifecycleStep.StepInfo> newInfo = new ArrayList<>();
    mLithoViewRule.setRoot(
        MountSpecLifecycleTester.create(mLithoViewRule.getContext()).steps(newInfo).build());

    mLithoViewRule.measure().layout();

    assertThat(getSteps(info))
        .describedAs("Should call the lifecycle methods on old instance in expected order")
        .containsExactly(LifecycleStep.ON_UNBIND, LifecycleStep.ON_UNMOUNT);

    assertThat(getSteps(newInfo))
        .describedAs("Should call the lifecycle methods on new instance in expected order")
        .containsExactly(
            LifecycleStep.ON_CREATE_TREE_PROP,
            LifecycleStep.ON_CALCULATE_CACHED_VALUE,
            LifecycleStep.ON_PREPARE,
            LifecycleStep.ON_MEASURE,
            LifecycleStep.ON_BOUNDS_DEFINED,
            LifecycleStep.ON_MOUNT,
            LifecycleStep.ON_BIND);

    assertThat(ComponentsPools.getMountContentPools().size())
        .describedAs("Should still contain only 1 content pool")
        .isEqualTo(1);
    assertThat(ComponentsPools.getMountContentPools().get(0).getName())
        .describedAs("Should still contain content pool from MountSpecLifecycleTester")
        .isEqualTo("MountSpecLifecycleTester");
  }

  @Test
  public void onSetRootWithPreallocatedMountContent_shouldCallLifecycleMethods() {
    final Looper looper = ShadowLooper.getLooperForThread(Thread.currentThread());
    final ComponentTree tree =
        ComponentTree.create(mLithoViewRule.getContext())
            .shouldPreallocateMountContentPerMountSpec(true)
            .preAllocateMountContentHandler(new LithoHandler.DefaultLithoHandler(looper))
            .build();
    mLithoViewRule.useComponentTree(tree);

    final List<LifecycleStep.StepInfo> info = new ArrayList<>();
    final Component component =
        PreallocatedMountSpecLifecycleTester.create(mLithoViewRule.getContext())
            .steps(info)
            .build();

    mLithoViewRule
        .getComponentTree()
        .setRootAndSizeSpec(
            component, mLithoViewRule.getWidthSpec(), mLithoViewRule.getHeightSpec());

    mLithoViewRule.measure();

    ShadowLooper.runUiThreadTasks();

    assertThat(getSteps(info))
        .describedAs("Should call the lifecycle methods on new instance in expected order")
        .containsExactly(
            LifecycleStep.ON_PREPARE,
            LifecycleStep.ON_MEASURE,
            LifecycleStep.ON_BOUNDS_DEFINED,
            LifecycleStep.ON_ATTACHED);

    assertThat(ComponentsPools.getMountContentPools().size())
        .describedAs("Should contain only 1 content pool")
        .isEqualTo(1);
    assertThat(ComponentsPools.getMountContentPools().get(0).getName())
        .describedAs("Should contain content pool from PreallocatedMountSpecLifecycleTester")
        .isEqualTo("PreallocatedMountSpecLifecycleTester");
  }

  @Test
  public void unmountAll_unmountAllItemsExceptRoot() {
    final List<LifecycleStep.StepInfo> info_child1 = new ArrayList<>();
    final List<LifecycleStep.StepInfo> info_child2 = new ArrayList<>();

    final Component root =
        Column.create(mLithoViewRule.getContext())
            .child(
                MountSpecLifecycleTester.create(mLithoViewRule.getContext())
                    .steps(info_child1)
                    .build())
            .child(
                MountSpecLifecycleTester.create(mLithoViewRule.getContext())
                    .steps(info_child2)
                    .build())
            .build();

    mLithoViewRule.setRoot(root).attachToWindow().measure().layout();

    final MountState mountState = mLithoViewRule.getLithoView().getMountState();
    LongSparseArray<MountItem> mountedItems = mountState.getIndexToItemMap();
    assertThat(mountedItems.size()).isGreaterThan(1);

    info_child1.clear();
    info_child2.clear();
    mLithoViewRule.getLithoView().unmountAllItems();

    assertThat(mountState.getIndexToItemMap().size()).isEqualTo(1);
    assertThat(mountState.getIndexToItemMap().get(0)).isNotNull();

    assertThat(LifecycleStep.getSteps(info_child1))
        .describedAs("Should call the following lifecycle methods in the following order:")
        .containsExactly(LifecycleStep.ON_UNBIND, LifecycleStep.ON_UNMOUNT);

    assertThat(LifecycleStep.getSteps(info_child2))
        .describedAs("Should call the following lifecycle methods in the following order:")
        .containsExactly(LifecycleStep.ON_UNBIND, LifecycleStep.ON_UNMOUNT);
  }

  @Test
  public void lifecycle_shouldUseGlobalPoolTrue_createsViewWithAppicationContext() {
    boolean isGlobalComponentsPoolEnabled = ComponentsConfiguration.isGlobalComponentsPoolEnabled;
    ComponentsConfiguration.isGlobalComponentsPoolEnabled = true;

    final Component component =
        ShouldUseGlobalPoolTrueMountSpecLifecycleTester.create(mLithoViewRule.getContext()).build();

    mLithoViewRule.setRoot(component);
    mLithoViewRule.attachToWindow().measure().layout();

    Object content =
        ComponentsPools.getMountContentPools()
            .get(0)
            .acquire(mLithoViewRule.getContext().getAndroidContext(), component);
    View view = null;
    if (content instanceof View) {
      view = (View) content;
    }

    assertThat(view != null).describedAs("Content pool should contain valid view").isEqualTo(true);

    assertThat(view.getContext() == mLithoViewRule.getContext().getApplicationContext())
        .describedAs("View acquired from the pool should have application context")
        .isEqualTo(true);

    ComponentsConfiguration.isGlobalComponentsPoolEnabled = isGlobalComponentsPoolEnabled;
  }

  @Test
  public void lifecycle_shouldUseGlobalPoolTrue_usesSamePoolForDifferentActivityContexts() {
    boolean isGlobalComponentsPoolEnabled = ComponentsConfiguration.isGlobalComponentsPoolEnabled;
    ComponentsConfiguration.isGlobalComponentsPoolEnabled = true;

    final Component componentOne =
        ShouldUseGlobalPoolTrueMountSpecLifecycleTester.create(mLithoViewRule.getContext()).build();

    ComponentsPools.maybePreallocateContent(
        mLithoViewRule.getContext().getAndroidContext(),
        componentOne,
        mLithoViewRule.getContext().getRecyclingMode());

    ComponentContext differentContext =
        new ComponentContext(new ContextWrapper(mLithoViewRule.getContext().getAndroidContext()));

    final Component componentTwo =
        ShouldUseGlobalPoolTrueMountSpecLifecycleTester.create(differentContext).build();

    ComponentsPools.maybePreallocateContent(
        differentContext.getAndroidContext(), componentTwo, differentContext.getRecyclingMode());

    assertThat(ComponentsPools.getMountContentPools().size())
        .describedAs("Should still contain only 1 content pool")
        .isEqualTo(1);

    ComponentsConfiguration.isGlobalComponentsPoolEnabled = isGlobalComponentsPoolEnabled;
  }

  @Test
  public void lifecycle_shouldUseGlobalPoolFalse_createsViewWithActivityContext() {
    boolean isGlobalComponentsPoolEnabled = ComponentsConfiguration.isGlobalComponentsPoolEnabled;
    ComponentsConfiguration.isGlobalComponentsPoolEnabled = true;

    final Component component =
        ShouldUseGlobalPoolFalseMountSpecLifecycleTester.create(mLithoViewRule.getContext())
            .build();

    mLithoViewRule.setRoot(component);
    mLithoViewRule.attachToWindow().measure().layout();

    Object content =
        ComponentsPools.getMountContentPools()
            .get(0)
            .acquire(mLithoViewRule.getContext().getAndroidContext(), component);
    View view = null;
    if (content instanceof View) {
      view = (View) content;
    }

    assertThat(view != null).describedAs("Content pool should contain valid view").isEqualTo(true);

    assertThat(view.getContext() == mLithoViewRule.getContext().getApplicationContext())
        .describedAs("View acquired from the pool should have activity context")
        .isEqualTo(true);

    ComponentsConfiguration.isGlobalComponentsPoolEnabled = isGlobalComponentsPoolEnabled;
  }

  @Test
  public void lifecycle_shouldUseGlobalPoolFalse_usesDifferentPoolsForDifferentActivityContexts() {
    boolean isGlobalComponentsPoolEnabled = ComponentsConfiguration.isGlobalComponentsPoolEnabled;
    ComponentsConfiguration.isGlobalComponentsPoolEnabled = true;

    final Component componentOne =
        ShouldUseGlobalPoolFalseMountSpecLifecycleTester.create(mLithoViewRule.getContext())
            .build();

    ComponentsPools.maybePreallocateContent(
        mLithoViewRule.getContext().getAndroidContext(),
        componentOne,
        mLithoViewRule.getContext().getRecyclingMode());

    ComponentContext differentContext =
        new ComponentContext(new ContextWrapper(mLithoViewRule.getContext().getAndroidContext()));

    final Component componentTwo =
        ShouldUseGlobalPoolFalseMountSpecLifecycleTester.create(differentContext).build();

    ComponentsPools.maybePreallocateContent(
        differentContext.getAndroidContext(), componentTwo, differentContext.getRecyclingMode());

    assertThat(ComponentsPools.getMountContentPools().size())
        .describedAs("Should contain 2 content pools")
        .isEqualTo(2);

    ComponentsConfiguration.isGlobalComponentsPoolEnabled = isGlobalComponentsPoolEnabled;
  }
}

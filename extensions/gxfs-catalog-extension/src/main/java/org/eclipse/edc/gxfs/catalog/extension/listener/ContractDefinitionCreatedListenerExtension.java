/*
 * Copyright (c) 2021 Microsoft Corporation
 *
 * This program and the accompanying materials are made available under the terms of the
 * Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 * Contributors: Microsoft Corporation - Initial implementation
 *
 */

package org.eclipse.edc.gxfs.catalog.extension.listener;

import org.eclipse.edc.connector.controlplane.asset.spi.index.AssetIndex;
import org.eclipse.edc.connector.controlplane.contract.spi.offer.store.ContractDefinitionStore;
import org.eclipse.edc.connector.controlplane.contract.spi.event.contractdefinition.ContractDefinitionCreated;
import org.eclipse.edc.connector.controlplane.policy.spi.store.PolicyDefinitionStore;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.event.EventRouter;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.Config;

public class ContractDefinitionCreatedListenerExtension implements ServiceExtension {

  @Inject
  private EventRouter eventRouter;

  @Override
  public void initialize(ServiceExtensionContext context) {
    var contractStore = context.getService(ContractDefinitionStore.class);
    var assetStore = context.getService(AssetIndex.class);
    var policyStore = context.getService(PolicyDefinitionStore.class);
    Config config = context.getConfig();
    eventRouter.registerSync(ContractDefinitionCreated.class,
                             new ContractDefinitionCreatedSubscriber(context.getMonitor(),
                                                                     contractStore,
                                                                     assetStore,
                                                                     policyStore,
                                                                     config));
  }

}

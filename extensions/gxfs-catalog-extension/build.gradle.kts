/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

plugins {
    `java-library`
    id("application")
}

dependencies {
   // api(libs.edc.control.plane.spi)
    api(libs.edc.contract.spi)
    api(libs.edc.asset.spi)
    implementation(libs.edc.http)
	implementation("com.googlecode.json-simple:json-simple:1.1")
	implementation("org.apache.httpcomponents:httpclient:4.5")
}

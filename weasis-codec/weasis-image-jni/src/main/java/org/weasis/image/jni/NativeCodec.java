/*******************************************************************************
 * Copyright (c) 2016 Weasis Team and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nicolas Roduit - initial API and implementation
 *******************************************************************************/
package org.weasis.image.jni;

import java.io.IOException;

import javax.imageio.ImageReadParam;
import javax.imageio.ImageWriteParam;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;

public interface NativeCodec {

    String readHeader(NativeImage nImage) throws IOException;

    String decompress(NativeImage nImage, ImageReadParam param) throws IOException;

    String compress(NativeImage nImage, ImageOutputStream ouputStream, ImageWriteParam param) throws IOException;

    void dispose();

    NativeImage buildImage(ImageInputStream iis) throws IOException;

}
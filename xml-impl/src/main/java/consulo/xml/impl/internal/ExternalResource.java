package consulo.xml.impl.internal;

import consulo.logging.Logger;
import consulo.util.io.FileUtil;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.net.URL;

public class ExternalResource
{
	private static final Logger LOG = Logger.getInstance(ExternalResource.class);

	private final String myFile;
	@Nonnull
	private final ClassLoader myClassLoader;
	private volatile String myResolvedResourcePath;

	public ExternalResource(String _file, @Nonnull ClassLoader _classLoader)
	{
		myFile = _file;
		myClassLoader = _classLoader;
	}

	public ExternalResource(String _file, ExternalResource baseResource)
	{
		this(_file, baseResource.myClassLoader);
	}

	public String directoryName()
	{
		int i = myFile.lastIndexOf('/');
		return i > 0 ? myFile.substring(0, i) : myFile;
	}

	@Nullable
	public String getResourceUrl()
	{
		String resolvedResourcePath = myResolvedResourcePath;
		if(resolvedResourcePath != null)
		{
			return resolvedResourcePath;
		}

		final URL resource = myClassLoader.getResource(myFile);

		if(resource == null)
		{
			String message = "Cannot find standard resource. filename:" + myFile + ", classLoader:" + myClassLoader;
			LOG.error(message);

			myResolvedResourcePath = null;
			return null;
		}

		String path = FileUtil.unquote(resource.toString());
		// this is done by FileUtil for windows
		path = path.replace('\\', '/');
		myResolvedResourcePath = path;
		return path;
	}

	@Override
	public boolean equals(Object o)
	{
		if(this == o)
		{
			return true;
		}
		if(o == null || getClass() != o.getClass())
		{
			return false;
		}

		ExternalResource resource = (ExternalResource) o;

		if(myClassLoader != resource.myClassLoader)
		{
			return false;
		}

		if(myFile != null ? !myFile.equals(resource.myFile) : resource.myFile != null)
		{
			return false;
		}

		return true;
	}

	@Override
	public int hashCode()
	{
		return myFile.hashCode();
	}

	@Override
	public String toString()
	{
		return myFile + " for " + myClassLoader;
	}
}

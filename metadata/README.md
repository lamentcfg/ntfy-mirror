# F-Droid Submission Guide

This directory contains metadata for submitting Ntfy Mirror to F-Droid.

## Steps to Submit to F-Droid

### 1. Get Your Signing Key Fingerprint

After building your first release, get the signing key fingerprint:

```bash
keytool -list -v -keystore keystore.jks -alias ntfy-mirror
```

Look for the SHA-256 fingerprint and format it as:
```
AA:BB:CC:DD:EE:FF:...
```

Update `AllowedAPKSigningKeys` in `com.lamentcfg.ntfymirror.yml` with this value.

### 2. Submit to F-Droid

1. Fork the [fdroiddata repository](https://gitlab.com/fdroid/fdroiddata)
2. Copy `com.lamentcfg.ntfymirror.yml` to `metadata/com.lamentcfg.ntfymirror.yml` in your fork
3. Run the lint checker:
   ```bash
   fdroid lint com.lamentcfg.ntfymirror
   fdroid readmeta
   ```
4. Create a merge request to fdroiddata

### 3. Alternative: IzzyOnDroid

IzzyOnDroid is easier and faster:

1. Ensure your GitHub releases have signed APKs
2. Submit via [IzzyOnDroid submission form](https://gitlab.com/IzzyOnDroid/repo/-/issues/new?issue[issue_type]=issue)
3. Provide your repo URL: `https://github.com/lamentcfg/ntfy-mirror`

IzzyOnDroid will automatically pull new releases from your GitHub releases.

## Files

- `com.lamentcfg.ntfymirror.yml` - F-Droid metadata file
- `README.md` - This file

## Resources

- [F-Droid Inclusion Policy](https://f-droid.org/en/docs/Inclusion_Policy/)
- [F-Droid Build Metadata Reference](https://f-droid.org/en/docs/Build_Metadata_Reference/)
- [IzzyOnDroid FAQ](https://apt.izzysoft.de/fdroid/index/info)

from sqlalchemy.orm import DeclarativeBase

NAMING_CONVENTION = {
    "ix": "ix_%(column_0_label)s",
    "uq": "uq_%(table_name)s_%(column_0_name)s",
    "ck": "ck_%(table_name)s_%(constraint_name)s",
    "fk": "fk_%(table_name)s_%(column_0_name)s_%(referred_table_name)s",
    "pk": "pk_%(table_name)s",
}


class Base(DeclarativeBase):
    pass


Base.metadata.naming_convention = NAMING_CONVENTION

# Import models so Alembic autogenerate sees every table.
# Plain imports (no attribute access) keep partial-init circulars safe.
import app.models.user  # noqa: E402,F401
import app.models.pack  # noqa: E402,F401
import app.models.marketplace  # noqa: E402,F401
import app.models.moderation  # noqa: E402,F401

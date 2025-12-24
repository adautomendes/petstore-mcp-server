#!/usr/bin/env python3

import argparse
import os
import random
import sys
from datetime import date, timedelta
from urllib.parse import urljoin

try:
    import requests
except ImportError as exc:  # pragma: no cover
    sys.stderr.write("The 'requests' package is required. Install it with 'pip install requests'.\n")
    raise

PET_NAMES = [
    "Bella",
    "Charlie",
    "Max",
    "Luna",
    "Rocky",
    "Milo",
    "Coco",
    "Buddy",
    "Lucy",
    "Daisy",
]

BREEDS = [
    "Labrador",
    "Poodle",
    "Bulldog",
    "Beagle",
    "Shiba Inu",
    "Pug",
    "German Shepherd",
    "Boxer",
    "Dachshund",
    "Mixed",
]

def random_age() -> int:
    return random.randint(1, 15)


def build_pet_payload(seq: int) -> dict:
    return {
        "nome": f"{random.choice(PET_NAMES)}-{seq}",
        "raca": random.choice(BREEDS),
        "idade": random_age(),
    }


def get_token(auth_base_url: str, user: str, password: str, timeout: int = 10) -> str:
    normalized = auth_base_url if auth_base_url.endswith("/") else f"{auth_base_url}/"
    endpoint = urljoin(normalized, "auth/login")
    try:
        resp = requests.post(endpoint, json={"user": user, "pass": password}, timeout=timeout)
        resp.raise_for_status()
        return resp.json().get("token")
    except requests.exceptions.RequestException as exc:
        raise RuntimeError(f"Failed to obtain token from {endpoint}: {exc}")


def create_pets(count: int, core_base_url: str, token: str) -> None:
    normalized = core_base_url if core_base_url.endswith("/") else f"{core_base_url}/"
    endpoint = urljoin(normalized, "pet")

    session = requests.Session()
    session.headers.update({"token": token})
    success = 0

    for idx in range(1, count + 1):
        payload = build_pet_payload(idx)
        try:
            response = session.post(endpoint, json=payload, timeout=10)
            response.raise_for_status()
            success += 1
            print(f"[{idx}/{count}] created pet id={response.json().get('_id') or response.json().get('id')} ({response.status_code})")
        except requests.exceptions.RequestException as exc:
            print(f"[{idx}/{count}] failed: {exc}")

    print(f"Finished: {success}/{count} pets created at {endpoint}")


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Populate Core service with dummy pets (uses Auth for token).")
    parser.add_argument(
        "--count",
        type=int,
        default=10,
        help="Number of pets to create (positive integer).",
    )
    parser.add_argument(
        "--auth-url",
        default=os.environ.get("AUTH_URL", "http://localhost:3000"),
        help="Base URL of the Auth API (default: http://localhost:3000 or $AUTH_URL).",
    )
    parser.add_argument(
        "--core-url",
        default=os.environ.get("CORE_URL", "http://localhost:3001"),
        help="Base URL of the Core API (default: http://localhost:3001 or $CORE_URL).",
    )
    parser.add_argument(
        "--user",
        default=os.environ.get("POPULATOR_USER", "admin"),
        help="Auth username (default: admin or $POPULATOR_USER).",
    )
    parser.add_argument(
        "--pass",
        dest="password",
        default=os.environ.get("POPULATOR_PASS", "admin"),
        help="Auth password (default: admin or $POPULATOR_PASS).",
    )

    args = parser.parse_args()

    if args.count <= 0:
        parser.error("count must be a positive integer")

    return args


def main() -> None:
    args = parse_args()
    try:
        token = get_token(args.auth_url, args.user, args.password)
    except RuntimeError as exc:
        print(f"Error getting token: {exc}")
        sys.exit(1)

    if not token:
        print("Auth did not return a token; aborting.")
        sys.exit(1)

    create_pets(args.count, args.core_url, token)


if __name__ == "__main__":
    main()

